package com.emc.mongoose.core.impl.load.executor;
// mongoose-common.jar
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.model.DataItemDst;
import com.emc.mongoose.core.api.data.model.DataItemSrc;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.req.RequestConfig;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataSource;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.model.metrics.IOStats;
import com.emc.mongoose.core.api.load.model.LoadState;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.load.model.metrics.BasicIOStats;
import com.emc.mongoose.core.impl.load.tasks.LoadCloseHook;
import com.emc.mongoose.core.impl.load.model.BasicLoadState;
import com.emc.mongoose.core.impl.load.model.DataItemSrcProducer;
//
import org.apache.commons.lang.StringUtils;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
extends DataItemSrcProducer<T>
implements LoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final int instanceNum, storageNodeCount, maxQueueSize;
	protected final String storageNodeAddrs[];
	//
	protected final Class<T> dataCls;
	protected final RunTimeConfig rtConfig;
	//
	protected final DataSource dataSrc;
	protected final RequestConfig<T> reqConfigCopy;
	protected final IOTask.Type loadType;
	//
	protected volatile DataItemDst<T> consumer = null;
	//
	private final long maxCount;
	protected final int totalConnCount;
	// METRICS section
	private final int metricsUpdatePeriodSec;
	private final IOStats ioStats;
	protected volatile IOStats.Snapshot lastStats = null;
	// STATES section //////////////////////////////////////////////////////////////////////////////
	private final Map<String, AtomicInteger> activeTasksStats = new HashMap<>();
	private LoadState<T> loadedPrevState = null;
	protected AtomicBoolean
		isStarted = new AtomicBoolean(false),
		isShutdown = new AtomicBoolean(false),
		isFinished = new AtomicBoolean(false);
	protected final AtomicLong
		counterSubm = new AtomicLong(0),
		countRej = new AtomicLong(0),
		counterResults = new AtomicLong(0);
	private final AtomicBoolean
		isInterrupted = new AtomicBoolean(false),
		isClosed = new AtomicBoolean(false);
	private final Lock lock = new ReentrantLock();
	private final Condition condProducerDone = lock.newCondition();
	private T lastDataItem;
	//
	private final Thread
		metricsDaemon = new Thread() {
			//
			{
				setDaemon(true);
			}
			//
			@Override
			public final void run() {
				try {
					if(metricsUpdatePeriodSec > 0) {
						while(!isClosed.get()) {
							logMetrics(Markers.PERF_AVG);
							TimeUnit.SECONDS.sleep(metricsUpdatePeriodSec);
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
				while(!isClosed.get() && !isInterrupted()) {
					//
					LockSupport.parkNanos(1);
					if(isDoneAllSubm() || isDoneMaxCount()) {
						lock.lock();
						try {
							condProducerDone.signalAll();
							if(LOG.isTraceEnabled(Markers.MSG)) {
								LOG.trace(
									Markers.MSG,
									"{}: done signal emitted because of condition, submitted: " +
									"{}, done: {}", getName(), counterSubm.get(), counterResults.get()
								);
							}
						} finally {
							lock.unlock();
						}
					}
					//
					LockSupport.parkNanos(1);
					lastStats = ioStats.getSnapshot();
					if(
						lastStats.getFailCount() > 1000000 &&
						lastStats.getFailRateLast() < lastStats.getSuccRateLast()
					) {
						LOG.fatal(
							Markers.ERR,
							"There's a more than 1M of failures and the failure rate is higher " +
							"than success rate for at least last {}[sec]. Exiting in order to " +
							"avoid the memory exhaustion. Please check your environment.",
							metricsUpdatePeriodSec
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
				}
			}
		};
	////////////////////////////////////////////////////////////////////////////////////////////////
	protected LoadExecutorBase(
		final Class<T> dataCls,
		final RunTimeConfig rtConfig, final RequestConfig<T> reqConfig, final String[] addrs,
		final int connCountPerNode, final int threadCount,
		final DataItemSrc<T> itemSrc, final long maxCount
	) {
		super(itemSrc, rtConfig.getBatchSize(), rtConfig.isDataSrcCircularEnabled());
		super.setDataItemDst(this);
		this.maxQueueSize = rtConfig.getTasksMaxQueueSize();
		//
		this.dataCls = dataCls;
		this.rtConfig = rtConfig;
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
		metricsUpdatePeriodSec = rtConfig.getLoadMetricsPeriodSec();
		final boolean flagServeJMX = rtConfig.getFlagServeJMX();
		if(flagServeJMX) {
			ioStats = new BasicIOStats(
				getName(), rtConfig.getRemotePortMonitor(), metricsUpdatePeriodSec
			);
		} else {
			ioStats = new BasicIOStats(getName(), 0, metricsUpdatePeriodSec);
		}
		lastStats = ioStats.getSnapshot();
		//
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		// prepare the nodes array
		storageNodeAddrs = addrs.clone();
		for(final String addr : storageNodeAddrs) {
			activeTasksStats.put(addr, new AtomicInteger(0));
		}
		dataSrc = reqConfig.getDataSource();
		//
		LoadCloseHook.add(this);
	}
	//
	@Override
	public final void run() {
		try {
			super.run();
		} finally {
			shutdown();
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void logMetrics(final Marker logMarker) {
		LOG.info(
			logMarker,
			Markers.PERF_SUM.equals(logMarker) ?
				"\"" + getName() + "\" summary: " + lastStats :
				lastStats
		);
	}
	//
	@Override
	public void start() {
		if(isStarted.compareAndSet(false, true)) {
			LOG.debug(Markers.MSG, "Starting {}", getName());
			ioStats.start();
			//
			if(rtConfig.isRunResumeEnabled()) {
				if (rtConfig.getRunMode().equals(Constants.RUN_MODE_STANDALONE)) {
					try {
						if(!RESTORED_STATES_MAP.containsKey(rtConfig.getRunId())) {
							BasicLoadState.restoreScenarioState(rtConfig);
						}
						setLoadState(BasicLoadState.<T>findStateByLoadNumber(instanceNum, rtConfig));
					} catch (final Exception e) {
						LogUtil.exception(LOG, Level.ERROR, e, "Unexpected failure");
					}
				}
			}
			//
			if(isFinished.get()) {
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
			if(counterResults.get() > 0) {
				setSkipCount(counterResults.get());
				setLastDataItem(loadedPrevState.getLastDataItem());
			}
			super.start();
			LOG.debug(Markers.MSG, "Started object producer {}", getName());
			//
			setName(getName());
			metricsDaemon.start();
			//
			LOG.debug(Markers.MSG, "Started \"{}\"", getName());
		} else {
			LOG.warn(Markers.ERR, "Second start attempt - skipped");
		}
	}
	//
	@Override
	public void interrupt() {
		//
		if(isFinished.get()) {
			return;
		}
		//
		if(isInterrupted.compareAndSet(false, true)) {
			final StringBuilder sb = new StringBuilder("Interrupt came from:");
			final StackTraceElement stackTrace[] = Thread.currentThread().getStackTrace();
			for(final StackTraceElement ste : stackTrace) {
				sb.append("\n\t").append(ste.toString());
			}
			LOG.debug(Markers.MSG, sb);
			metricsDaemon.interrupt();
			shutdown();
			try {
				reqConfigCopy.close(); // disables connection drop failures
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to close the request configurator");
			}
			// releasing the blocked join() methods, if any
			lock.lock();
			try {
				condProducerDone.signalAll();
				LOG.debug(
					Markers.MSG, "{}: done signal emitted by the interruption", getName()
				);
			} finally {
				lock.unlock();
			}
			//
			try {
				if(isStarted.get()) { // if was executing
					lastStats = ioStats.getSnapshot();
					ioStats.close();
					logMetrics(Markers.PERF_SUM); // provide summary metrics
					// calculate the efficiency and report
					final float
						loadDurMicroSec = lastStats.getElapsedTime(),
						eff = lastStats.getDurationSum() / loadDurMicroSec / totalConnCount;
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
			if(consumer != null) {
				try {
					consumer.close();
				} catch(final IOException e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "Failed to close the consumer \"{}\"", consumer
					);
				}
			}
			LOG.debug(Markers.MSG, "{} interrupted", getName());
		} else {
			LOG.debug(Markers.MSG, "{} was already interrupted", getName());
		}

	}
	//
	@Override
	public void setDataItemDst(final DataItemDst<T> itemDst) {
		this.consumer = itemDst;
		LOG.debug(
			Markers.MSG, "Appended the consumer \"{}\" for producer \"{}\"", itemDst, getName()
		);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Consumer implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void put(final T dataItem)
	throws InterruptedException, RemoteException {
		if(counterSubm.get() + countRej.get() >= maxCount) {
			LOG.debug(
				Markers.MSG, "{}: all tasks has been submitted ({}) or rejected ({})", getName(),
				counterSubm.get(), countRej.get()
			);
			shutdown();
			return;
		}
		// prepare the I/O task instance (make the link between the data item and load type)
		final String nextNodeAddr = storageNodeCount == 1 ? storageNodeAddrs[0] : getNextNode();
		final IOTask<T> ioTask = getIOTask(dataItem, nextNodeAddr);
		// try to sleep while underlying connection pool becomes more free if it's going too fast
		// warning: w/o such sleep the behaviour becomes very ugly
		while(
			!isShutdown.get() && !isInterrupted.get() &&
			counterSubm.get() - counterResults.get() >= maxQueueSize
		) {
			LockSupport.parkNanos(1);
		}
		//
		try {
			if(null == submitReq(ioTask)) {
				throw new RejectedExecutionException("Null future returned");
			}
			counterSubm.incrementAndGet();
			activeTasksStats.get(nextNodeAddr).incrementAndGet(); // increment node's usage counter
		} catch(final RejectedExecutionException e) {
			if(!isInterrupted.get()) {
				countRej.incrementAndGet();
				LogUtil.exception(LOG, Level.DEBUG, e, "Rejected the I/O task {}", ioTask);
			}
		}
	}
	//
	@Override
	public int put(final List<T> srcBuff, final int from, final int to)
	throws InterruptedException, RemoteException {
		final long dstLimit = maxCount - counterSubm.get() - countRej.get();
		final int srcLimit = to - from;
		int n = 0, m;
		if(dstLimit > 0) {
			if(dstLimit < srcLimit) {
				return put(srcBuff, from, from + (int) dstLimit);
			} else {
				// select the target node
				final String nextNodeAddr = storageNodeCount == 1 ?
					storageNodeAddrs[0] : getNextNode();
				// prepare the I/O tasks list (make the link between the data item and load type)
				final List<IOTask<T>> ioTaskBuff = new ArrayList<>(srcLimit);
				if(srcLimit > getIOTasks(srcBuff, from, to, ioTaskBuff, nextNodeAddr)) {
					LOG.warn(Markers.ERR, "Produced less I/O tasks then expected ({})", srcLimit);
				}
				// submit all I/O tasks
				while(n < srcLimit) {
					// don't fill the connection pool as fast as possible, this may cause a failure
					while(
						!isShutdown.get() && !isInterrupted.get() &&
						counterSubm.get() - counterResults.get() >= maxQueueSize
					) {
						LockSupport.parkNanos(1);
					}
					//
					try {
						m = submitReqs(ioTaskBuff, n, srcLimit);
						if(m < 1) {
							throw new RejectedExecutionException("No I/O tasks submitted");
						} else {
							n += m;
						}
						counterSubm.addAndGet(m);
						// increment node's usage counter
						activeTasksStats.get(nextNodeAddr).addAndGet(m);
					} catch(final RejectedExecutionException e) {
						if(!isInterrupted.get()) {
							m = srcLimit - n;
							countRej.addAndGet(m);
							LogUtil.exception(LOG, Level.DEBUG, e, "Rejected {} I/O tasks", m);
						}
					}
				}
			}
		} else {
			shutdown();
			if(srcLimit > 0) {
				countRej.addAndGet(srcLimit);
				LOG.debug(Markers.MSG, "Rejected {} I/O tasks", srcLimit);
			}
		}
		return n;
	}
	//
	protected abstract IOTask<T> getIOTask(final T dataItem, final String nextNodeAddr);
	//
	protected int getIOTasks(
		final List<T> dataItems, final int from, final int to,
		final List<IOTask<T>> dstTaskBuff, final String nextNodeAddr
	) {
		for(final T dataItem : dataItems) {
			if(dataItem == null) {
				break;
			} else {
				dstTaskBuff.add(getIOTask(dataItem, nextNodeAddr));
			}
		}
		return to - from;
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
		if(status == IOTask.Status.SUCC) {
			lastDataItem = dataItem;
			// update the metrics with success
			markSucc(ioTask);
			// put the data item to the consumer and finally check for the finish state
			try {
				// is this an end of consumer-producer chain?
				if(consumer == null) {
					if(LOG.isInfoEnabled(Markers.DATA_LIST)) {
						LOG.info(Markers.DATA_LIST, dataItem);
					}
				} else { // put to the consumer
					if(LOG.isTraceEnabled(Markers.MSG)) {
						LOG.trace(
							Markers.MSG, "Going to put the data item {} to the consumer {}",
							dataItem, consumer
						);
					}
					consumer.put(dataItem);
					if(LOG.isTraceEnabled(Markers.MSG)) {
						LOG.trace(
							Markers.MSG, "The data item {} is passed to the consumer {} successfully",
							dataItem, consumer
						);
					}
				}
			} catch(final InterruptedException e) {
				LOG.debug(Markers.MSG, "Interrupted while submitting to the consumer");
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to put the data item \"{}\" to \"{}\"",
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
			ioStats.markFail();
		}
		//
		counterResults.incrementAndGet();
	}
	//
	@Override
	public final int handleResults(
		final List<IOTask<T>> ioTasks, final int from, final int to
	) {
		// producing was interrupted?
		if(isInterrupted.get()) {
			return 0;
		}
		//
		final int n = to - from;
		if(n > 0) {
			final List<T> passedItems = new ArrayList<>(n);
			final String nodeAddr = ioTasks.get(from).getNodeAddr();
			activeTasksStats.get(nodeAddr).addAndGet(-n);
			//
			IOTask<T> ioTask;
			for(int i = from; i < to; i ++) {
				ioTask = ioTasks.get(i);
				if(IOTask.Status.SUCC.equals(ioTask.getStatus())) {
					lastDataItem = ioTask.getDataItem();
					markSucc(ioTask);
					passedItems.add(lastDataItem);
				} else {
					ioStats.markFail();
				}
			}
			// put the data items to the consumer and finally check for the finish state
			try {
				// is this an end of consumer-producer chain?
				if(consumer == null) {
					for(final T passedItem : passedItems) {
						if(LOG.isInfoEnabled(Markers.DATA_LIST)) {
							LOG.info(Markers.DATA_LIST, passedItem);
						}
					}
				} else { // put to the consumer
					if(LOG.isTraceEnabled(Markers.MSG)) {
						LOG.trace(
							Markers.MSG, "Going to put {} data items to the consumer {}",
							passedItems.size(), consumer
						);
					}
					consumer.put(passedItems, 0, passedItems.size());
					if(LOG.isTraceEnabled(Markers.MSG)) {
						LOG.trace(
							Markers.MSG, "{} data items were passed to the consumer {} successfully",
							passedItems.size(), consumer
						);
					}
				}
			} catch(final InterruptedException e) {
				LOG.debug(Markers.MSG, "Interrupted while submitting to the consumer");
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to put {} data items to \"{}\"",
					passedItems.size(), consumer
				);
			} catch(final RejectedExecutionException e) {
				if(LOG.isTraceEnabled(Markers.ERR)) {
					LogUtil.exception(
						LOG, Level.TRACE, e, "\"{}\" rejected {} data items", consumer,
						passedItems.size()
					);
				}
			}
			//
			counterResults.addAndGet(n);
		}
		//
		return n;
	}
	//
	private void markSucc(final IOTask<T> ioTask) {
		final int
			duration = ioTask.getDuration(),
			latency = ioTask.getLatency();
		// update the metrics with success
		if(latency > duration) {
			LOG.warn(
				Markers.ERR, "{}: latency {} is more than duration: {}", ioTask, latency, duration
			);
		}
		ioStats.markSucc(ioTask.getCountBytesDone(), duration, latency);
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, "Task #{}: successful, {}/{}", ioTask.hashCode(),
				lastStats.getSuccCount(), ioTask.getCountBytesDone()
			);
		}
	}
	//
	@Override
	public void setLoadState(final LoadState<T> state) {
		if(state != null) {
			if(state.isLoadFinished(rtConfig)) {
				isFinished.compareAndSet(false, true);
				LOG.warn(Markers.MSG, "\"{}\": nothing to do more", getName());
				return;
			}
			// apply parameters from loadState to current load executor
			final IOStats.Snapshot statsSnapshot = state.getStatsSnapshot();
			final long
				countSucc = statsSnapshot.getSuccCount(),
				countFail = statsSnapshot.getFailCount();
			counterSubm.addAndGet(countSucc + countFail);
			counterResults.set(countSucc + countFail);
			ioStats.markSucc(
				countSucc, statsSnapshot.getByteCount(), statsSnapshot.getDurationValues(),
				statsSnapshot.getLatencyValues()
			);
			ioStats.markFail(countFail);
			ioStats.markElapsedTime(statsSnapshot.getElapsedTime());
			loadedPrevState = state;
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public LoadState<T> getLoadState()
	throws RemoteException {
		return new BasicLoadState.Builder<T, BasicLoadState<T>>()
			.setLoadNumber(instanceNum)
			.setRunTimeConfig(rtConfig)
			.setStatsSnapshot(lastStats)
			.setLastDataItem(lastDataItem)
			.build();
	}
	//
	@Override
	public IOStats.Snapshot getStatsSnapshot() {
		return lastStats;
	}
	//
	private boolean isDoneMaxCount() {
		return counterResults.get() >= maxCount;
	}
	//
	private boolean isDoneAllSubm() {
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, "{}: shut down flag: {}, results: {}, submitted: {}", getName(),
				isShutdown.get(), counterResults.get(), counterSubm.get()
			);
		}
		return isShutdown.get() && counterResults.get() >= counterSubm.get();
	}
	//
	@Override
	public final void shutdown() {
		if(isStarted.get())
			if(isShutdown.compareAndSet(false, true)) {
				super.interrupt(); // stop the source producing right now
				LOG.debug(
					Markers.MSG, "Stopped the producing from \"{}\" for \"{}\"",
					itemSrc, getName()
				);
			} else {
				LOG.debug(
					Markers.MSG,
					"{}: ignoring the shutdown invocation because is already shut down", getName()
				);
		} else {
			LOG.debug(
				Markers.MSG,
				"{}: ignoring the shutdown invocation because has not been started yet", getName()
			);
		}
	}
	//
	@Override
	public final void await()
	throws InterruptedException {
		await(Long.MAX_VALUE, TimeUnit.DAYS);
	}
	//
	@Override
	public final void await(final long timeOut, final TimeUnit timeUnit)
	throws InterruptedException {
		if(isInterrupted.get() || isClosed.get()) {
			return;
		}
		//
		long timeOutMicroSec = timeUnit.toMicros(timeOut);
		if(loadedPrevState != null) {
			if(isFinished.get()) {
				return;
			}
			timeOutMicroSec -= loadedPrevState.getStatsSnapshot().getElapsedTime();
		}
		//
		lock.lock();
		try {
			LOG.debug(
				Markers.MSG, "{}: await for the done condition at most for {}[us]",
				getName(), timeOutMicroSec
			);
			if(condProducerDone.await(timeOutMicroSec, TimeUnit.MICROSECONDS)) {
				LOG.debug(Markers.MSG, "{}: await for the done condition is finished", getName());
			} else {
				LOG.debug(
					Markers.MSG, "{}: await timeout, unhandled results left: {}",
					getName(), counterSubm.get() - counterResults.get()
				);
			}
		} finally {
			lock.unlock();
		}
	}
	//
	@Override
	public void close()
		throws IOException {
		// interrupt the producing
		if(isClosed.compareAndSet(false, true)) {
			LOG.debug(Markers.MSG, "Invoked close for {}", getName());
			try {
				interrupt();
				releaseDaemon.interrupt();
			} finally {
				LoadCloseHook.del(this);
				if(loadedPrevState != null) {
					if(RESTORED_STATES_MAP.containsKey(rtConfig.getRunId())) {
						RESTORED_STATES_MAP.get(rtConfig.getRunId()).remove(loadedPrevState);
					}
				}
			}
			LOG.debug(Markers.MSG, "\"{}\" closed successfully", getName());
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
	public final RequestConfig<T> getRequestConfig() {
		return reqConfigCopy;
	}
	//
	@Override
	public final String toString() {
		return getName();
	}
}
