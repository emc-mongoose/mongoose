package com.emc.mongoose.core.impl.load.executor;
//
import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
// mongoose-common.jar
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtils;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.util.DataSource;
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.model.Producer;
import com.emc.mongoose.core.api.load.model.LoadState;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.io.task.BasicIOTask;
import com.emc.mongoose.core.impl.load.model.BasicDataItemGenerator;
import com.emc.mongoose.core.impl.load.model.AsyncConsumerBase;
import com.emc.mongoose.core.impl.load.model.FileProducer;
import com.emc.mongoose.core.impl.load.model.PersistentAccumulatorProducer;
import com.emc.mongoose.core.impl.load.tasks.LoadCloseHook;
import com.emc.mongoose.core.impl.load.model.BasicLoadState;
//
import org.apache.commons.lang.StringUtils;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
//
import javax.management.MBeanServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/**
 Created by kurila on 15.10.14.
 */
public abstract class LoadExecutorBase<T extends DataItem>
extends AsyncConsumerBase<T>
implements LoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static int RELEASE_TIMEOUT_MILLISEC = 100;
	private final static Map<String, List<LoadState>> DESERIALIZED_STATES = new ConcurrentHashMap<>();
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
	protected volatile PersistentAccumulatorProducer<T> itemsBuff = null;
	//
	private final long maxCount;
	private final int totalConnCount;
	// METRICS section
	protected final MetricRegistry metrics = new MetricRegistry();
	protected Counter counterSubm, counterRej, counterReqFail;
	protected Meter throughPut, reqBytes;
	protected Histogram respLatency;
	//
	protected final MBeanServer mBeanServer;
	protected final JmxReporter jmxReporter;
	//
	private final Map<String, AtomicInteger> activeTasksStats = new HashMap<>();
	private final BlockingQueue<IOTask<T>> ioTaskSpentQueue;
	//
	private LoadState currState = null;
	//  these parameters are necessary for pause/resume Mongoose w/ SIGSTOP and SIGCONT signals
	private long lastTimeBeforeTermination = 0;
	private long elapsedTimeInPause = 0;
	//
	private static final List<String> IMMUTABLE_PARAMS = new ArrayList<>();
	//
	static {
		initializeImmutableParams();
	}
	//
	private final Thread
		metricsDaemon = new Thread() {
			//
			{ setDaemon(true); } // do not block process exit
			//
			@Override
			public final void run() {
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
				IOTask<T> spentIOTask;
				try {
					while(!isClosed.get()) {
						// 1st check for done condition
						if(isDoneAllSubm() || isDoneMaxCount()) {
							if(lock.tryLock(RELEASE_TIMEOUT_MILLISEC, TimeUnit.MILLISECONDS)) {
								try {
									condProducerDone.signalAll();
									LOG.debug(
										Markers.MSG, "{}: done/interrupted signal emitted",
										getName()
									);
								} finally {
									lock.unlock();
								}
							} else {
								LOG.warn(
									Markers.ERR, "{}: failed to acquire the lock in close method",
									getName()
								);
							}
						}
						// try to get a task for releasing into the pool
						spentIOTask = ioTaskSpentQueue.poll(
							RELEASE_TIMEOUT_MILLISEC, TimeUnit.MILLISECONDS
						);
						// release the next task if necessary
						if(spentIOTask != null) {
							spentIOTask.release();
						}
					}
				} catch(final InterruptedException ignored) {
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
		final int connCountPerNode, final String listFile, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias
	) {
		super(
			maxCount, rtConfig.getRunRequestQueueSize(),
			rtConfig.getRunSubmitTimeOutMilliSec()
		);
		//
		this.dataCls = dataCls;
		this.rtConfig = rtConfig;
		instanceNum = NEXT_INSTANCE_NUM.getAndIncrement();
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
		ioTaskSpentQueue = new ArrayBlockingQueue<>(maxQueueSize);
		dataSrc = reqConfig.getDataSource();
		//
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
					producer = new FileProducer<>(maxCount, listFile, dataCls);
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
			producer = reqConfig.getAnyDataProducer(maxCount, addrs[0]);
			LOG.debug(Markers.MSG, "{} will use {} as data items producer", getName(), producer);
		}
		//
		if(producer != null) {
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
	private static void initializeImmutableParams() {
		IMMUTABLE_PARAMS.add("run.mode");
		IMMUTABLE_PARAMS.add("run.version");
		IMMUTABLE_PARAMS.add("scenario.name");
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
			countReqSucc = throughPut.getCount(),
			countReqFail = counterReqFail.getCount();
		final double
			//  If Mongoose's run was paused w/ SIGSTOP signal then calculate meanTP and meanBW w/
			//  values from Meter's library implementation after resumption. All metrics will be calculated correctly.
			//  If Mongoose's run was paused w/ SIGINT signal then calculate these metrics w/o Meter's library
			//  implementation. Only average values in TP and BW will be calculated correctly.
			//  Other values will be gradually recovered.
			meanTP = (System.nanoTime() - lastTimeBeforeTermination > TimeUnit.SECONDS.toNanos(1))
				? throughPut.getMeanRate() : countReqSucc / elapsedTime * TimeUnit.SECONDS.toNanos(1),
			oneMinTP = throughPut.getOneMinuteRate(),
			fiveMinTP = throughPut.getFiveMinuteRate(),
			fifteenMinTP = throughPut.getFifteenMinuteRate(),
			meanBW = (System.nanoTime() - lastTimeBeforeTermination > TimeUnit.SECONDS.toNanos(1))
				? reqBytes.getMeanRate() : reqBytes.getCount() / elapsedTime * TimeUnit.SECONDS.toNanos(1),
			oneMinBW = reqBytes.getOneMinuteRate(),
			fiveMinBW = reqBytes.getFiveMinuteRate(),
			fifteenMinBW = reqBytes.getFifteenMinuteRate();
		final Snapshot respLatencySnapshot = respLatency.getSnapshot();
		//
		final String message = Markers.PERF_SUM.equals(logMarker) ?
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
			) :
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
			);
		LOG.info(logMarker, message);
	}
	//
	private final AtomicLong tsStart = new AtomicLong(-1);
	//
	@Override
	public void start() {
		if(tsStart.compareAndSet(-1, System.nanoTime())) {
			LOG.debug(Markers.MSG, "Starting {}", getName());
			// init metrics
			final Clock userClock = getMetricsClock();
			counterSubm = metrics.counter(MetricRegistry.name(getName(), METRIC_NAME_SUBM));
			counterRej = metrics.counter(MetricRegistry.name(getName(), METRIC_NAME_REJ));
			counterReqFail = metrics.counter(MetricRegistry.name(getName(), METRIC_NAME_FAIL));
			throughPut = metrics.register(MetricRegistry.name(getName(), METRIC_NAME_REQ, METRIC_NAME_TP),
				new Meter(userClock));
			reqBytes = metrics.register(MetricRegistry.name(getName(), METRIC_NAME_REQ, METRIC_NAME_BW),
				new Meter(userClock));
			respLatency = metrics.histogram(MetricRegistry.name(getName(), METRIC_NAME_REQ, METRIC_NAME_LAT));
			//
			final String fullStateFileName = Paths.get(RunTimeConfig.DIR_ROOT,
					Constants.DIR_LOG, RunTimeConfig.getContext().getRunId())
						.resolve(Constants.STATES_FILE).toString();
			final File statesFile = new File(fullStateFileName);
			if (statesFile.exists()) {
				if (!DESERIALIZED_STATES.containsKey(rtConfig.getRunId())) {
					loadStateFromFile(fullStateFileName);
				}
				final List<LoadState> loadStates = DESERIALIZED_STATES.get(rtConfig.getRunId());
				//  apply parameters from loadState to current load executor
				for (final LoadState state : loadStates) {
					if (state.getLoadNumber() == instanceNum) {
					if (state.getLoadNumber() == loadNum) {
						if (isImmutableParamsChanged(state.getRunTimeConfig())) {
							LOG.warn(Markers.MSG, "\"{}\": configuration immutability violated.",
								getName());
						}
						counterSubm.inc(state.getCountSucc() + state.getCountFail());
						counterResults.set(state.getCountSucc() + state.getCountFail());
						counterReqFail.inc(state.getCountFail());
						throughPut.mark(state.getCountSucc());
						reqBytes.mark(state.getCountBytes());
						currState = state;
						if (isLoadExecutorFinished(currState)) {
							LOG.info(Markers.MSG, "\"{}\": nothing to do more", getName());
							return;
						}
						break;
					}
				}
			} else {
				LOG.debug(Markers.MSG, "File with state of run with run.id: \"{}\" wasn't found. Starting new run...",
					RunTimeConfig.getContext().getRunId());
			}
			//
			releaseDaemon.setName("releaseDaemon<" + getName() + ">");
			releaseDaemon.start();
			//
			super.start();
			//
			if(producer == null) {
				LOG.debug(Markers.MSG, "{}: using an external data items producer", getName());
				if(itemsBuff != null) {
					try {
						itemsBuff.close();
					} catch(final IOException e) {
						LogUtil.exception(LOG, Level.WARN, e, "Failed to close the items buffer file");
					}
					itemsBuff.start();
				}
			} else {
				//
				try {
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
	private boolean isLoadExecutorFinished(final LoadState state) {
		final RunTimeConfig localRunTimeConfig = state.getRunTimeConfig();
		final long loadTimeMillis = (localRunTimeConfig.getLoadLimitTimeUnit().
				toMillis(localRunTimeConfig.getLoadLimitTimeValue())) > 0
				? (localRunTimeConfig.getLoadLimitTimeUnit().
				toMillis(localRunTimeConfig.getLoadLimitTimeValue())) : Long.MAX_VALUE;
		final long stateTimeMillis = state.getLoadElapsedTimeUnit().
				toMillis(state.getLoadElapsedTimeValue());
		return isDoneMaxCount() || (stateTimeMillis >= loadTimeMillis);
	}
	//
	private Clock getMetricsClock() {
		return new Clock() {
			private final long tickInterval = TimeUnit.SECONDS.toNanos(1);
			//
			//  This Clock's implementation provides correct time for calculating different metrics
			//  after resumption Mongoose's run w/ SIGCONT signal.
			@Override
			public long getTick() {
				final long currTime = System.nanoTime();
				if (lastTimeBeforeTermination > 0) {
					if (currTime - lastTimeBeforeTermination > tickInterval) {
						elapsedTimeInPause += currTime - lastTimeBeforeTermination;
						lastTimeBeforeTermination = currTime;
						return currTime - elapsedTimeInPause;
					}
				}
				lastTimeBeforeTermination = currTime;
				return currTime - elapsedTimeInPause;
			}
		};
	}
	//
	private boolean isImmutableParamsChanged(final RunTimeConfig loadStateConfig) {
		for (final String param : IMMUTABLE_PARAMS) {
			if (!rtConfig.getString(param).equals(loadStateConfig.getString(param))) {
				return true;
			}
		}
		return false;
	}
	//
	private void loadStateFromFile(final String fullStateFileName) {
		try (final FileInputStream fis = new FileInputStream(fullStateFileName)) {
			try (final ObjectInputStream ois = new ObjectInputStream(fis)) {
				LOG.info(Markers.MSG, "Run with run.id: \"{}\" was resumed",
						rtConfig.getRunId());
				final List<LoadState> loadStates = (List<LoadState>) ois.readObject();
				DESERIALIZED_STATES.put(rtConfig.getRunId(), loadStates);
			}
		} catch (final FileNotFoundException e) {
			LOG.debug(Markers.MSG, "File with state of load job[s] with run.id: \"{}\" wasn't found. " +
				"Starting new run...", rtConfig.getRunId());
		} catch (final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e,
				"Failed to load state of load job[s] with run.id: \"{}\" from \"{}\" file." +
				"Starting new run...", rtConfig.getRunId(), fullStateFileName);
		} catch (final ClassNotFoundException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to deserialize state of load job[s]." +
				"Starting new run...");
		}
	}
	//
	@Override
	public final void interrupt() {
		if (currState != null) {
			if (isLoadExecutorFinished(currState))
				return;
		}
		//
		if(isInterrupted.compareAndSet(false, true)) {
			metricsDaemon.interrupt();
			shutdown();
			// releasing the blocked join() methods, if any
			try {
				if(lock.tryLock(rtConfig.getRunReqTimeOutMilliSec(), TimeUnit.MILLISECONDS)) {
					try {
						condProducerDone.signalAll();
						LOG.debug(Markers.MSG, "{}: done/interrupted signal emitted", getName());
					} finally {
						lock.unlock();
					}
				} else {
					LOG.warn(Markers.ERR, "{}: failed to acquire the lock in close method", getName());
				}
			} catch(final InterruptedException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "{}: Interrupted while acquiring the lock", getName()
				);
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
	@Override
	public void submit(final T dataItem)
	throws InterruptedException, RemoteException, RejectedExecutionException {
		try {
			if(isStarted.get()) {
				super.submit(dataItem);
			} else { // accumulate until started
				synchronized(itemsBuff) {
					if(itemsBuff == null) {
						itemsBuff = new PersistentAccumulatorProducer<>(
							dataCls, rtConfig, this.maxCount
						);
						itemsBuff.setConsumer(this);
						LOG.debug(
							Markers.MSG, "{}: not started yet, consuming into the temporary file"
						);
					}
				}
				itemsBuff.submit(dataItem);
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
			Thread.sleep(1);
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
	@SuppressWarnings("unchecked")
	protected BasicIOTask<T> getIOTask(final T dataItem, final String nextNodeAddr) {
		return BasicIOTask.getInstance(this, dataItem, nextNodeAddr);
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
		final int latency = ioTask.getLatency();
		if(status == IOTask.Status.SUCC) {
			// update the metrics with success
			throughPut.mark();
			if(latency > 0) {
				respLatency.update(latency);
			}
			durTasksSum.addAndGet(ioTask.getRespTimeDone() - ioTask.getReqTimeStart());
			reqBytes.mark(ioTask.getTransferSize());
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Task #{}: successful, {}/{}",
					ioTask.hashCode(), throughPut.getCount(), ioTask.getTransferSize()
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
			counterReqFail.inc();
		}
		//
		counterResults.incrementAndGet();
	}
	//
	@Override
	public LoadState getLoadState()
	throws RemoteException {
		final long prevElapsedTime = currState != null ?
				currState.getLoadElapsedTimeUnit().toNanos(currState.getLoadElapsedTimeValue()) : 0;
		return new BasicLoadState(
			instanceNum, rtConfig, throughPut.getCount(), counterReqFail.getCount(),
			reqBytes.getCount(), counterSubm.getCount(), TimeUnit.NANOSECONDS,
			prevElapsedTime + (System.nanoTime() - tsStart.get())
		);
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
		try {
			if(producer != null) {
				producer.interrupt(); // stop the producing right now
				LOG.debug(
					Markers.MSG, "Stopped the producer \"{}\" for \"{}\"", producer, getName()
				);
			}
			if(itemsBuff != null) {
				itemsBuff.interrupt();
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to stop the producer: {}", producer);
		} finally {
			super.shutdown();
		}
	}
	//
	@Override
	public void close()
	throws IOException {
		// interrupt the producing
		if(isClosed.compareAndSet(false, true)) {
			LOG.debug(Markers.MSG, "Invoked close for {}", getName());
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
				ioTaskSpentQueue.clear();
				BasicIOTask.INSTANCE_POOL_MAP.put(this, null); // dispose I/O tasks pool
				if(jmxReporter != null) {
					jmxReporter.close();
				}
				LOG.debug(Markers.MSG, "JMX reported closed");
				LoadCloseHook.del(this);
				DESERIALIZED_STATES.remove(rtConfig.getRunId());
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
		long t = System.currentTimeMillis(), timeOutMilliSec;
		if (currState != null) {
			if (isLoadExecutorFinished(currState))
				return;
			timeOutMilliSec = timeUnit.toMillis(timeOut) -
				currState.getLoadElapsedTimeUnit().toMillis(currState.getLoadElapsedTimeValue());
		} else {
			timeOutMilliSec = timeUnit.toMillis(timeOut);
		}
		//
		if(lock.tryLock(timeOut, timeUnit)) {
			try {
				t = System.currentTimeMillis() - t; // the count of time wasted for locking
				LOG.debug(
					Markers.MSG, "{}: wait for the done condition at most for {}[ms]",
					getName(), timeOutMilliSec - t
				);
				if(condProducerDone.await(timeOutMilliSec - t, TimeUnit.MILLISECONDS)) {
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
		} else {
			LOG.warn(Markers.ERR, "Failed to acquire the lock for the join method");
		}
	}
}
