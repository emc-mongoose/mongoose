package com.emc.mongoose.core.impl.load.executor;
//
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
// mongoose-common.jar
import com.codahale.metrics.UniformReservoir;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtils;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.model.DataItemInput;
import com.emc.mongoose.core.api.data.model.FileDataItemOutput;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.req.RequestConfig;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataSource;
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.model.Producer;
import com.emc.mongoose.core.api.load.model.LoadState;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.data.model.CSVFileItemOutput;
import com.emc.mongoose.core.impl.io.task.BasicIOTask;
import com.emc.mongoose.core.impl.load.model.AsyncConsumerBase;
import com.emc.mongoose.core.impl.load.model.util.metrics.ResumableUserTimeClock;
import com.emc.mongoose.core.impl.load.tasks.LoadCloseHook;
import com.emc.mongoose.core.impl.load.model.BasicLoadState;
//
import com.emc.mongoose.core.impl.load.model.DataItemInputProducer;
import org.apache.commons.lang.StringUtils;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
//
import javax.management.MBeanServer;
import java.io.IOException;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
/**
 Created by kurila on 15.10.14.
 */
public abstract class LoadExecutorBase<T extends DataItem>
extends AsyncConsumerBase<T>
implements LoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final int instanceNum, connCountPerNode, storageNodeCount;
	protected final String storageNodeAddrs[];
	//
	protected final Class<T> dataCls;
	protected final RunTimeConfig rtConfig;
	//
	protected final DataSource dataSrc;
	protected final RequestConfig<T> reqConfigCopy;
	protected final IOTask.Type loadType;
	//
	protected volatile Producer<T> producer = null;
	protected volatile Consumer<T> consumer;
	protected volatile FileDataItemOutput<T> itemsFileBuff = null;
	//
	private final long maxCount;
	private final int totalConnCount;
	// METRICS section
	protected final MetricRegistry metrics = new MetricRegistry();
	protected Counter counterSubm, counterRej;
	protected Meter throughPutSucc, throughPutFail, reqBytes;
	protected Histogram reqDuration, respLatency;
	//
	protected final MBeanServer mBeanServer;
	protected final JmxReporter jmxReporter;
	//
	private final Map<String, AtomicInteger> activeTasksStats = new HashMap<>();
	//
	private LoadState<T> currState = null;
	private ResumableUserTimeClock clock = new ResumableUserTimeClock();
	private AtomicBoolean isLoadFinished = new AtomicBoolean(false);
	//
	private T lastDataItem;
	private final DataItemInput<T> itemsSrc;
	//
	private final Thread
		metricsDaemon = new Thread() {
			//
			{ setDaemon(true); } // do not block process exit
			//
			@Override
			public final void run() {
				// required for int tests passing
				ThreadContext.put(RunTimeConfig.KEY_RUN_ID, rtConfig.getRunId());
				//
				final long metricsUpdatePeriodMilliSec = TimeUnit.SECONDS.toMillis(
					rtConfig.getLoadMetricsPeriodSec()
				);
				try {
					if(metricsUpdatePeriodMilliSec > 0) {
						while(!isClosed.get()) {
							logMetrics(Markers.PERF_AVG);
							Thread.sleep(metricsUpdatePeriodMilliSec);
						}
					} else {
						Thread.sleep(Long.MAX_VALUE);
					}
				} catch(final InterruptedException e) {
					LOG.debug(Markers.MSG, "{}: interrupted", getName());
				}
			}
		},
		releaseDaemon = new Thread() {
			//
			{ setDaemon(true); }
			//
			@Override
			public final void run() {
				while(!isClosed.get()) {
					//
					LockSupport.parkNanos(1);
					//
					if(isDoneAllSubm() || isDoneMaxCount()) {
						lock.lock();
						try {
							condProducerDone.signalAll();
							LOG.trace(
								Markers.MSG, "{}: done/interrupted signal emitted",
								getName()
							);
						} finally {
							lock.unlock();
						}
					}
					//
					LockSupport.parkNanos(1);
					//
					if(
						throughPutFail.getCount() > 1000000 &&
						throughPutSucc.getOneMinuteRate() < throughPutFail.getOneMinuteRate()
					) {
						LOG.fatal(
							Markers.ERR,
							"There's a more than 1M of failures and the failure rate is higher " +
							"than success rate for at least 1 minute. Exiting in order to avoid " +
							"the memory exhaustion."
						);
						try {
							LoadExecutorBase.this.close();
						} catch(final IOException e) {
							LogUtil.exception(LOG, Level.WARN, e, "Failed to close the load job");
						}
						break;
					}
					//
					LockSupport.parkNanos(1);
					//
				}
			}
		};
	// STATES section
	protected final AtomicLong
		durTasksSum = new AtomicLong(0),
		counterResults = new AtomicLong(0);
	private final AtomicBoolean
		isInterrupted = new AtomicBoolean(false),
		isClosed = new AtomicBoolean(false);
	private final Lock lock = new ReentrantLock();
	private final Condition condProducerDone = lock.newCondition();
	//
	protected LoadExecutorBase(
		final Class<T> dataCls,
		final RunTimeConfig rtConfig, final RequestConfig<T> reqConfig, final String[] addrs,
		final int connCountPerNode, final DataItemInput<T> itemSrc, final long maxCount
	) {
		super(
			maxCount, rtConfig.getTasksMaxQueueSize(),
			rtConfig.isShuffleItemsEnabled(), rtConfig.getBatchSize()
		);
		//
		this.dataCls = dataCls;
		this.rtConfig = rtConfig;
		this.itemsSrc = itemSrc;
		if (!INSTANCE_NUMBERS.containsKey(rtConfig.getRunId())) {
			INSTANCE_NUMBERS.put(rtConfig.getRunId(), new AtomicInteger(0));
		}
		instanceNum = INSTANCE_NUMBERS.get(rtConfig.getRunId()).getAndIncrement();
		storageNodeCount = addrs.length;
		//
		setName(
			Integer.toString(instanceNum) + '-' +
				StringUtils.capitalize(reqConfig.getAPI().toLowerCase()) + '-' +
				StringUtils.capitalize(reqConfig.getLoadType().toString().toLowerCase()) +
				(maxCount > 0 ? Long.toString(maxCount) : "") + '-' +
				Integer.toString(connCountPerNode) + 'x' + Integer.toString(storageNodeCount)
		);
		//
		totalConnCount = connCountPerNode * storageNodeCount;
		//
		RequestConfig<T> reqConfigClone = null;
		try {
			reqConfigClone = reqConfig.clone();
		} catch(final CloneNotSupportedException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to clone the request config");
		} finally {
			this.reqConfigCopy = reqConfigClone;
		}
		loadType = reqConfig.getLoadType();
		//
		counterSubm = metrics.counter(MetricRegistry.name(getName(), METRIC_NAME_SUBM));
		counterRej = metrics.counter(MetricRegistry.name(getName(), METRIC_NAME_REJ));
		final String runMode = rtConfig.getRunMode();
		final boolean flagServeRemoteIfStandalone = rtConfig.getFlagServeIfNotLoadServer();
		if(Constants.RUN_MODE_STANDALONE.equals(runMode) && !flagServeRemoteIfStandalone) {
			LOG.debug(
				Markers.MSG, "{}: running in the \"{}\" mode, remote serving is disabled",
				getName(), runMode
			);
			mBeanServer = null;
			jmxReporter = null;
		} else {
			LOG.debug(
				Markers.MSG, "{}: running in the \"{}\" mode, remote serving flag is \"{}\"",
				getName(), runMode, flagServeRemoteIfStandalone
			);
			mBeanServer = ServiceUtils.getMBeanServer(rtConfig.getRemotePortExport());
			jmxReporter = JmxReporter.forRegistry(metrics)
				//.convertDurationsTo(TimeUnit.MICROSECONDS)
				//.convertRatesTo(TimeUnit.SECONDS)
				.registerWith(mBeanServer)
				.build();
			jmxReporter.start();
		}
		//
		this.connCountPerNode = connCountPerNode;
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		// prepare the nodes array
		storageNodeAddrs = addrs.clone();
		for(final String addr : storageNodeAddrs) {
			activeTasksStats.put(addr, new AtomicInteger(0));
		}
		dataSrc = reqConfig.getDataSource();
		/*
		if(listFile != null && listFile.length() > 0) {
			final Path dataItemsListPath = Paths.get(listFile);
			if(!Files.exists(dataItemsListPath)) {
				LOG.warn(
					Markers.ERR, "Data items source file \"{}\" doesn't exist",
					dataItemsListPath
				);
			} else if(!Files.isReadable(dataItemsListPath)) {
				LOG.warn(
					Markers.ERR, "Data items source file \"{}\" is not readable",
					dataItemsListPath
				);
			} else {
				try {
					producer = new DataItemInputProducer<>(
						new CSVFileItemInput<>(Paths.get(listFile), dataCls)
					);
					LOG.debug(
						Markers.MSG, "{} will use file-based producer: {}", getName(), listFile
					);
				} catch(final NoSuchMethodException | IOException e) {
					LogUtil.exception(
						LOG, Level.FATAL, e,
						"Failed to create file producer for the class \"{}\" and src file \"{}\"",
						dataCls.getName(), listFile
					);
				}
			}
		} else if(loadType == IOTask.Type.CREATE) {
			try {
				producer = new BasicDataItemGenerator<>(
					dataCls, maxCount, sizeMin, sizeMax, sizeBias
				);
				LOG.debug(Markers.MSG, "{} will use new data items producer", getName());
			} catch(final NoSuchMethodException e) {
				LogUtil.exception(
					LOG, Level.FATAL, e,
					"Failed to create new data items producer for class \"{}\"",
					dataCls.getName()
				);
			}
		} else {
			producer = reqConfig.getContainerListInput(maxCount, addrs[0]);
			LOG.debug(Markers.MSG, "{} will use {} as data items producer", getName(), producer);
		}*/
		if(itemsSrc != null) {
			producer = new DataItemInputProducer<>(itemsSrc);
			try {
				producer.setConsumer(this);
			} catch(final RemoteException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Unexpected failure");
			}
		}
		//
		LoadCloseHook.add(this);
	}
	//
	@Override
	public final String toString() {
		return getName();
	}
	//
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Producer implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void logMetrics(final Marker logMarker) {
		// duration when load is done
		final double elapsedTime = (currState != null) ?
			currState.getLoadElapsedTimeUnit().
				toNanos(currState.getLoadElapsedTimeValue()) + (System.nanoTime() - tsStart.get())
			: (System.nanoTime() - tsStart.get());
		//
		final long
			countReqSucc = throughPutSucc.getCount(),
			countReqFail = throughPutFail.getCount();
		final double
			//  If Mongoose's run was paused w/ SIGSTOP signal then calculate meanTP and meanBW w/
			//  values from Meter's library implementation after resumption.
			//  All metrics will be calculated correctly.
			//  If Mongoose's run was paused w/ SIGINT signal then calculate
			//  these metrics w/o Meter's library implementation.
			//  Only average values in TP and BW will be calculated correctly.
			//  Other values will be gradually recovered.
			meanTP = countReqSucc / elapsedTime * TimeUnit.SECONDS.toNanos(1),
			oneMinTP = throughPutSucc.getOneMinuteRate(),
			fiveMinTP = throughPutSucc.getFiveMinuteRate(),
			fifteenMinTP = throughPutSucc.getFifteenMinuteRate(),
			meanBW = reqBytes.getCount() / elapsedTime * TimeUnit.SECONDS.toNanos(1),
			oneMinBW = reqBytes.getOneMinuteRate(),
			fiveMinBW = reqBytes.getFiveMinuteRate(),
			fifteenMinBW = reqBytes.getFifteenMinuteRate();
		final Snapshot
			reqDurationSnapshot = reqDuration.getSnapshot(),
			respLatencySnapshot = respLatency.getSnapshot();
		//
		if(Markers.PERF_SUM.equals(logMarker)) {
			LOG.info(
				logMarker,
				String.format(
					LogUtil.LOCALE_DEFAULT, MSG_FMT_SUM_METRICS,
					//
					getName(),
					countReqSucc,
					countReqFail == 0 ?
						Long.toString(countReqFail) :
						(float) countReqSucc / countReqFail > 100 ?
							String.format(LogUtil.INT_YELLOW_OVER_GREEN, countReqFail) :
							String.format(LogUtil.INT_RED_OVER_GREEN, countReqFail),
					//
					(int) respLatencySnapshot.getMean(),
					(int) respLatencySnapshot.getMin(),
					(int) respLatencySnapshot.getMedian(),
					(int) respLatencySnapshot.getMax(),
					//
					meanTP, oneMinTP, fiveMinTP, fifteenMinTP,
					meanBW / MIB, oneMinBW / MIB, fiveMinBW / MIB, fifteenMinBW / MIB
				)
			);
		} else if(Markers.PERF_AVG.equals(logMarker)) {
			LOG.info(
				logMarker,
				String.format(
					LogUtil.LOCALE_DEFAULT, MSG_FMT_METRICS,
					//
					countReqSucc, counterSubm.getCount() - counterResults.get(),
					countReqFail == 0 ?
						Long.toString(countReqFail) :
						(float) countReqSucc / countReqFail > 100 ?
							String.format(LogUtil.INT_YELLOW_OVER_GREEN, countReqFail) :
							String.format(LogUtil.INT_RED_OVER_GREEN, countReqFail),
					//
					(int) respLatencySnapshot.getMean(),
					(int) respLatencySnapshot.getMin(),
					(int) respLatencySnapshot.getMedian(),
					(int) respLatencySnapshot.getMax(),
					//
					meanTP, oneMinTP, fiveMinTP, fifteenMinTP,
					meanBW / MIB, oneMinBW / MIB, fiveMinBW / MIB, fifteenMinBW / MIB
				)
			);
		}
	}
	//
	private final AtomicLong tsStart = new AtomicLong(-1);
	//
	@Override
	public void start() {
		if(tsStart.compareAndSet(-1, System.nanoTime())) {
			LOG.debug(Markers.MSG, "Starting {}", getName());
			// init remaining (load exec time dependent) metrics
			throughPutSucc = metrics.register(
				MetricRegistry.name(getName(), METRIC_NAME_REQ, METRIC_NAME_TP),
				new Meter(clock)
			);
			throughPutFail = metrics.register(
				MetricRegistry.name(getName(), METRIC_NAME_FAIL),
				new Meter(clock)
			);
			reqBytes = metrics.register(
				MetricRegistry.name(getName(), METRIC_NAME_REQ, METRIC_NAME_BW),
				new Meter(clock)
			);
			respLatency = metrics.register(
				MetricRegistry.name(getName(), METRIC_NAME_REQ, METRIC_NAME_LAT),
				new Histogram(new UniformReservoir())
			);
			reqDuration = metrics.register(
				MetricRegistry.name(getName(), METRIC_NAME_REQ, METRIC_NAME_DUR),
				new Histogram(new UniformReservoir())
			);
			//
			if(rtConfig.isRunResumeEnabled()) {
				if (rtConfig.getRunMode().equals(Constants.RUN_MODE_STANDALONE)) {
					try {
						if(!RESTORED_STATES_MAP.containsKey(rtConfig.getRunId())) {
							BasicLoadState.restoreScenarioState(rtConfig);
						}
						setLoadState(BasicLoadState.findStateByLoadNumber(instanceNum, rtConfig));
					} catch (final Exception e) {
						LogUtil.exception(LOG, Level.ERROR, e, "Unexpected failure");
					}
				}
			}
			//
			if(isLoadFinished.get()) {
				try {
					close();
				} catch (final IOException e) {
					LogUtil.exception(LOG, Level.ERROR, e,
						"Couldn't close the load executor \"{}\"", getName());
				}
				return;
			}
			//
			releaseDaemon.setName("releaseDaemon<" + getName() + ">");
			releaseDaemon.start();
			//
			super.start();
			//
			itemsFileLock.lock();
			try {
				if(itemsFileBuff != null) {
					itemsFileBuff.close();
					final Path itemsFilePath = itemsFileBuff.getFilePath();
					LOG.debug(
						Markers.MSG, "{}: accumulated for input {} of data items metadata in the temporary file \"{}\"",
						getName(), SizeUtil.formatSize(itemsFilePath.toFile().length()), itemsFilePath
					);
					isShutdown.compareAndSet(true, false); // cancel if shut down before start
					producer = new DataItemInputProducer<>(itemsFileBuff.getInput());
				}
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to close the items buffer file");
			} finally {
				itemsFileLock.unlock();
			}
			//
			if(producer == null) {
				LOG.debug(Markers.MSG, "{}: using an external data items producer", getName());
			} else {
				try {
					producer.setConsumer(this);
					if(
						producer instanceof DataItemInputProducer &&
						counterResults.get() > 0
					) {
						final DataItemInputProducer<T> inputProducer
							= (DataItemInputProducer<T>) producer;
						inputProducer.setSkippedItemsCount(counterResults.get());
						inputProducer.setLastDataItem(currState.getLastDataItem());
					}
					producer.start();
					LOG.debug(Markers.MSG, "Started object producer {}", producer);
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to start the producer");
				}
			}
			//
			metricsDaemon.setName(getName());
			metricsDaemon.start();
			//
			LOG.debug(Markers.MSG, "Started \"{}\"", getName());
		} else {
			LOG.warn(Markers.ERR, "Second start attempt - skipped");
		}
	}
	//
	@Override
	public final void interrupt() {
		if(isLoadFinished.get()) {
			return;
		}
		if(isInterrupted.compareAndSet(false, true)) {
			metricsDaemon.interrupt();
			shutdown();
			// releasing the blocked join() methods, if any
			lock.lock();
			try {
				condProducerDone.signalAll();
				LOG.debug(Markers.MSG, "{}: done/interrupted signal emitted", getName());
			} finally {
				lock.unlock();
			}
			//
			try {
				final long tsStartNanoSec = tsStart.get();
				if(tsStartNanoSec > 0) { // if was executing
					logMetrics(Markers.PERF_SUM); // provide summary metrics
					// calculate the efficiency and report
					final float
						loadDurMicroSec = (float) (System.nanoTime() - tsStart.get()) / 1000,
						eff = durTasksSum.get() / (loadDurMicroSec * totalConnCount);
					LOG.debug(
						Markers.MSG,
						String.format(
							LogUtil.LOCALE_DEFAULT,
							"%s: load execution duration: %3.3f[sec], efficiency estimation: %3.1f[%%]",
							getName(), loadDurMicroSec / 1e6, 100 * eff
						)
					);
				} else {
					LOG.debug(Markers.ERR, "{}: trying to interrupt while not started", getName());
				}
			} catch(final Throwable t) {
				t.printStackTrace(System.err);
			}
			//
			LOG.debug(Markers.MSG, "{} interrupted", getName());
		} else {
			LOG.debug(Markers.MSG, "{} was already interrupted", getName());
		}

	}
	//
	@Override
	public final Consumer<T> getConsumer() {
		return consumer;
	}
	//
	@Override
	public final void setConsumer(final Consumer<T> consumer) {
		this.consumer = consumer;
		LOG.debug(
			Markers.MSG, "Appended the consumer \"{}\" for producer \"{}\"", consumer, getName()
		);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Consumer implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final Lock itemsFileLock = new ReentrantLock();
	//
	@Override
	public void submit(final T dataItem)
	throws InterruptedException, RemoteException, RejectedExecutionException {
		try {
			if(isStarted.get()) {
				super.submit(dataItem);
			} else { // accumulate until started
				itemsFileLock.lock();
				try {
					if(itemsFileBuff == null) {
						itemsFileBuff = new CSVFileItemOutput<>(dataCls);
						LOG.debug(
							Markers.MSG,
							"{}: not started yet, consuming into the temporary file @ {}",
							getName(), itemsFileBuff.getFilePath()
						);
					}
					itemsFileBuff.write(dataItem);
				} catch(final IOException | NoSuchMethodException e) {
					throw new RejectedExecutionException(e);
				} finally {
					itemsFileLock.unlock();
				}
			}
		} catch(final RejectedExecutionException e) {
			counterRej.inc();
			throw e;
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected final void submitSync(final T dataItem)
	throws InterruptedException, RemoteException {
		if(counterSubm.getCount() + counterRej.getCount() >= maxCount) {
			LOG.debug(
				Markers.MSG, "{}: all tasks has been submitted ({}) or rejected ({})", getName(),
				counterSubm.getCount(), counterRej.getCount()
			);
			super.interrupt();
			return;
		}
		// prepare the I/O task instance (make the link between the data item and load type)
		final String nextNodeAddr = storageNodeCount == 1 ? storageNodeAddrs[0] : getNextNode();
		final IOTask<T> ioTask = getIOTask(dataItem, nextNodeAddr);
		// try to sleep while underlying connection pool becomes more free if it's going too fast
		// warning: w/o such sleep the behaviour becomes very ugly
		while(
			!isAllSubm.get() && !isInterrupted.get() &&
			counterSubm.getCount() - counterResults.get() >= maxQueueSize
		) {
			LockSupport.parkNanos(1);
		}
		//
		try {
			if(null == submit(ioTask)) {
				throw new RejectedExecutionException("Null future returned");
			}
			counterSubm.inc();
			activeTasksStats.get(nextNodeAddr).incrementAndGet(); // increment node's usage counter
		} catch(final RejectedExecutionException e) {
			if(!isInterrupted.get()) {
				counterRej.inc();
				LogUtil.exception(LOG, Level.DEBUG, e, "Rejected the I/O task {}", ioTask);
			}
		}
	}
	//
	protected IOTask<T> getIOTask(final T dataItem, final String nextNodeAddr) {
		return new BasicIOTask<>(this, dataItem, nextNodeAddr);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Balancing implementation
	////////////////////////////////////////////////////////////////////////////////////////////////
	// round-robin variant:
	/*private final AtomicInteger rountRobinCounter = new AtomicInteger(0);
	protected String getNextNode() {
		return storageNodeAddrs[rountRobinCounter.incrementAndGet() % storageNodeCount];
	}*/
	protected String getNextNode() {
		String bestNode = null;
		//final StringBuilder sb = new StringBuilder("Active tasks stats: ");
		int minActiveTaskCount = Integer.MAX_VALUE, nextActiveTaskCount;
		for(final String nextNode : storageNodeAddrs) {
			nextActiveTaskCount = activeTasksStats.get(nextNode).get();
			//sb.append(nextNode).append("=").append(nextActiveTaskCount).append(", ");
			if(nextActiveTaskCount < minActiveTaskCount) {
				minActiveTaskCount = nextActiveTaskCount;
				bestNode = nextNode;
			}
		}
		//LOG.trace(LogUtil.MSG, sb.append("best: ").append(bestNode).toString());
		return bestNode;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void handleResult(final IOTask<T> ioTask)
	throws RemoteException {
		// producing was interrupted?
		if(isInterrupted.get()) {
			return;
		}
		// update the metrics
		activeTasksStats.get(ioTask.getNodeAddr()).decrementAndGet();
		final IOTask.Status status = ioTask.getStatus();
		final T dataItem = ioTask.getDataItem();
		final int duration = ioTask.getDuration(), latency = ioTask.getLatency();
		if(status == IOTask.Status.SUCC) {
			lastDataItem = dataItem;
			// update the metrics with success
			throughPutSucc.mark();
			if(latency > 0) {
				respLatency.update(latency);
			}
			if(duration > 0) {
				reqDuration.update(duration);
				durTasksSum.addAndGet(duration);
			}
			reqBytes.mark(ioTask.getTransferSize());
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Task #{}: successful, {}/{}",
					ioTask.hashCode(), throughPutSucc.getCount(), ioTask.getTransferSize()
				);
			}
			// feed the data item to the consumer and finally check for the finish state
			try {
				// is this an end of consumer-producer chain?
				if(consumer == null) {
					LOG.info(Markers.DATA_LIST, dataItem);
				} else { // feed to the consumer
					if(LOG.isTraceEnabled(Markers.MSG)) {
						LOG.trace(
							Markers.MSG, "Going to feed the data item {} to the consumer {}",
							dataItem, consumer
						);
					}
					consumer.submit(dataItem);
					if(LOG.isTraceEnabled(Markers.MSG)) {
						LOG.trace(
							Markers.MSG, "The data item {} is passed to the consumer {} successfully",
							dataItem, consumer
						);
					}
				}
			} catch(final InterruptedException e) {
				LOG.debug(Markers.MSG, "Interrupted");
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to submit the data item \"{}\" to \"{}\"",
					dataItem, consumer
				);
			} catch(final RejectedExecutionException e) {
				if(LOG.isTraceEnabled(Markers.ERR)) {
					LogUtil.exception(
						LOG, Level.TRACE, e, "\"{}\" rejected the data item \"{}\"", consumer,
						dataItem
					);
				}
			}
		} else {
			throughPutFail.mark();
		}
		//
		counterResults.incrementAndGet();
	}
	//
	@Override
	public void setLoadState(final LoadState<T> state) {
		if (state != null) {
			if (state.isLoadFinished(rtConfig)) {
				isLoadFinished.compareAndSet(false, true);
				LOG.warn(Markers.MSG, "\"{}\": nothing to do more", getName());
				return;
			}
			//  apply parameters from loadState to current load executor
			counterSubm.inc(state.getCountSucc() + state.getCountFail());
			counterResults.set(state.getCountSucc() + state.getCountFail());
			throughPutFail.mark(state.getCountFail());
			throughPutSucc.mark(state.getCountSucc());
			reqBytes.mark(state.getCountBytes());
			for(final long durationValue : state.getDurationValues()) {
				respLatency.update(durationValue);
			}
			for(final long latencyValue : state.getLatencyValues()) {
				respLatency.update(latencyValue);
			}
			currState = state;
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public LoadState<T> getLoadState()
	throws RemoteException {
		final long prevElapsedTime = currState != null ?
			currState.getLoadElapsedTimeUnit().toNanos(currState.getLoadElapsedTimeValue()) : 0;
		final LoadState.Builder<T, BasicLoadState<T>> stateBuilder = new BasicLoadState.Builder<>();
		stateBuilder.setLoadNumber(instanceNum)
			.setRunTimeConfig(rtConfig)
			.setCountSucc(throughPutSucc == null ? 0 : throughPutSucc.getCount())
			.setCountFail(throughPutFail == null ? 0 : throughPutFail.getCount())
			.setCountBytes(reqBytes == null ? 0 : reqBytes.getCount())
			.setCountSubm(counterSubm == null ? 0 : counterSubm.getCount())
			.setLoadElapsedTimeValue(
				tsStart.get() < 0 ? 0 : prevElapsedTime + (System.nanoTime() - tsStart.get())
			)
			.setLoadElapsedTimeUnit(TimeUnit.NANOSECONDS)
			.setDurationValues(
				reqDuration == null ? new long[]{} : reqDuration.getSnapshot().getValues()
			)
			.setLatencyValues(
				respLatency == null ? new long[]{} : respLatency.getSnapshot().getValues()
			)
			.setLastDataItem(lastDataItem);
		//
		return stateBuilder.build();
	}
	//
	private boolean isDoneMaxCount() {
		return counterResults.get() >= maxCount;
	}
	//
	private boolean isDoneAllSubm() {
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.debug(
				Markers.MSG, "{}: all submitted: {}, results: {}, submitted: {}",
				getName(), isAllSubm.get(), counterResults.get(), counterSubm.getCount()
			);
		}
		return isAllSubm.get() && counterResults.get() >= counterSubm.getCount();
	}
	//
	@Override
	public final void shutdown() {
		if(isStarted.get() && !isShutdown.get()) {
			try {
				if(producer != null) {
					producer.interrupt(); // stop the producing right now
					LOG.debug(
						Markers.MSG, "Stopped the producer \"{}\" for \"{}\"", producer, getName()
					);
				}
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to stop the producer: {}", producer);
			} finally {
				super.shutdown();
			}
		} else {
			LOG.debug(
				Markers.MSG,
				"{}: ignoring the shutdown invocation because has not been started yet",
				getName()
			);
		}
	}
	//
	@Override
	public void close()
	throws IOException {
		// interrupt the producing
		if(isClosed.compareAndSet(false, true)) {
			LOG.debug(Markers.MSG, "Invoked close for {}", getName());
			if(itemsFileBuff != null) {
				itemsFileBuff.getFilePath().toFile().delete();
			}
			interrupt();
			try {
				LOG.debug(Markers.MSG, "Forcing the shutdown");
				reqConfigCopy.close(); // disables connection drop failures
				super.close();
				if(consumer != null) {
					consumer.shutdown(); // poison the consumer
					LOG.debug(Markers.MSG, "Consumer \"{}\" has been poisoned", consumer);
				}
			} catch(final IllegalStateException | RejectedExecutionException e) {
				LogUtil.exception(LOG, Level.DEBUG, e, "Failed to poison the consumer");
			} finally {
				releaseDaemon.interrupt();
				if(jmxReporter != null) {
					jmxReporter.close();
				}
				LOG.debug(Markers.MSG, "JMX reported closed");
				LoadCloseHook.del(this);
				if (currState != null) {
					if (RESTORED_STATES_MAP.containsKey(rtConfig.getRunId())) {
						RESTORED_STATES_MAP.get(rtConfig.getRunId()).remove(currState);
					}
				}
				LOG.debug(Markers.MSG, "\"{}\" closed successfully", getName());
			}
		} else {
			LOG.debug(
				Markers.MSG,
				"Not closing \"{}\" because it has been closed before already", getName()
			);
		}
	}
	//
	@Override
	protected final void finalize()
	throws Throwable {
		try {
			close();
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "{}: failed to close", getName());
		} finally {
			super.finalize();
		}
	}
	//
	@Override
	public final Producer<T> getProducer() {
		return producer;
	}
	//
	@Override
	public final long getMaxCount() {
		return maxCount;
	}
	//
	@Override
	public final RequestConfig<T> getRequestConfig() {
		return reqConfigCopy;
	}
	//
	@Override
	public final void await()
	throws InterruptedException {
		join(Long.MAX_VALUE);
	}
	//
	@Override
	public final void await(final long timeOut, final TimeUnit timeUnit)
	throws InterruptedException {
		if(isInterrupted.get() || isClosed.get()) {
			return;
		}
		//
		final long timeOutMilliSec;
		if (currState != null) {
			if (isLoadFinished.get())
				return;
			timeOutMilliSec = timeUnit.toMillis(timeOut) -
				currState.getLoadElapsedTimeUnit().toMillis(currState.getLoadElapsedTimeValue());
		} else {
			timeOutMilliSec = timeUnit.toMillis(timeOut);
		}
		//
		lock.lock();
		try {
			LOG.debug(
				Markers.MSG, "{}: wait for the done condition at most for {}[ms]",
				getName(), timeOutMilliSec
			);
			if(condProducerDone.await(timeOutMilliSec, TimeUnit.MILLISECONDS)) {
				LOG.debug(Markers.MSG, "{}: join finished", getName());
			} else {
				LOG.debug(
					Markers.MSG, "{}: join timeout, unhandled results left: {}",
					getName(), counterSubm.getCount() - counterResults.get()
				);
			}
		} finally {
			lock.unlock();
		}
	}
}
