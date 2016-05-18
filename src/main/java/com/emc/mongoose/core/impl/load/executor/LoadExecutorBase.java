package com.emc.mongoose.core.impl.load.executor;
// mongoose-common.jar

import com.emc.mongoose.common.concurrent.LifeCycle;
import com.emc.mongoose.common.concurrent.NamingThreadFactory;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.io.conf.IoConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.base.ItemBuffer;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.ContentSource;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.load.balancer.Balancer;
import com.emc.mongoose.core.api.load.barrier.Throttle;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.executor.MixedLoadExecutor;
import com.emc.mongoose.core.api.load.model.LoadState;
import com.emc.mongoose.core.api.load.model.metrics.IOStats;
import com.emc.mongoose.core.impl.item.base.LimitedQueueItemBuffer;
import com.emc.mongoose.core.impl.load.balancer.BasicNodeBalancer;
import com.emc.mongoose.core.impl.load.barrier.ActiveTasksThrottle;
import com.emc.mongoose.core.impl.load.model.BasicItemGenerator;
import com.emc.mongoose.core.impl.load.model.BasicLoadState;
import com.emc.mongoose.core.impl.load.model.LoadRegistry;
import com.emc.mongoose.core.impl.load.model.metrics.BasicIOStats;
import com.emc.mongoose.core.impl.load.tasks.processors.ChartPackage;
import com.emc.mongoose.core.impl.load.tasks.processors.PolyLineManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

// mongoose-core-api.jar
// mongoose-core-impl.jar
//
//
/**
 Created by kurila on 15.10.14.
 */
public abstract class LoadExecutorBase<T extends Item>
extends BasicItemGenerator<T>
implements LoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final int instanceNum, storageNodeCount;
	protected final String storageNodeAddrs[];
	//
	protected final AppConfig appConfig;
	//
	protected final ContentSource dataSrc;
	protected final IoConfig<? extends Item, ? extends Container<? extends Item>>
		ioConfigCopy;
	protected final LoadType loadType;
	//
	protected volatile Output<T> consumer = null;
	//
	protected final long sizeLimit;
	protected final int totalThreadCount;
	protected final Throttle<T> activeTasksThrottle;
	// METRICS section
	protected final int metricsPeriodSec;
	protected final boolean preconditionFlag;
	protected IOStats ioStats;
	protected volatile IOStats.Snapshot lastStats = null;
	// STATES section //////////////////////////////////////////////////////////////////////////////
	private Balancer<String> nodeBalancer = null;
	private LoadState<T> loadedPrevState = null;
	protected final AtomicBoolean
		isStarted = new AtomicBoolean(false),
		isShutdown = new AtomicBoolean(false),
		isInterrupted = new AtomicBoolean(false),
		isClosed = new AtomicBoolean(false);
	protected boolean isLimitReached = false;
	protected final AtomicLong
		counterSubm = new AtomicLong(0),
		countRej = new AtomicLong(0),
		counterResults = new AtomicLong(0);
	protected T lastItem;
	protected final Object state = new Object();
	protected final ItemBuffer<T> itemOutBuff;
	////////////////////////////////////////////////////////////////////////////////////////////////
	protected final List<Runnable> mgmtTasks = new LinkedList<>();
	private final ThreadPoolExecutor mgmtExecutor;
	//
	public final static int MAX_FAIL_COUNT = 100_000;
	private final class StatsRefreshTask
	implements Runnable {
		@Override
		public final void run() {
			final Thread currThread = Thread.currentThread();
			currThread.setName("statsRefresh<" + getName() + ">");
			try {
				while(!currThread.isInterrupted()) {
					synchronized(ioStats) {
						ioStats.wait(1_000);
					}
					refreshStats();
					checkForBadState();
					LockSupport.parkNanos(1_000_000);
				}
			} catch(final InterruptedException ignored) {
			}
		}
	}
	//
	private final class ResultsDispatcher
	implements Runnable {
		@Override
		public final void run() {
			final Thread currThread = Thread.currentThread();
			currThread.setName("resultsDispatcher<" + getName() + ">");
			try {
				if(isCircular) {
					while(!allItemsProducedFlag) {
						LockSupport.parkNanos(1_000);
					}
				}
				while(!currThread.isInterrupted()) {
					postProcessItems();
					LockSupport.parkNanos(1_000);
				}
			} catch(final InterruptedException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Interrupted");
			} finally {
				LOG.debug(Markers.MSG, "{}: results dispatched finished", getName());
			}
		}
	}
	//
	private final class LogMetricsTask
	implements Runnable {
		@Override
		public final void run() {
			Thread.currentThread().setName(LoadExecutorBase.this.getName());
			PolyLineManager polyLineManager = new PolyLineManager();
			while(!isInterrupted.get()) {
				logMetrics(Markers.PERF_AVG);
				if (true) { // todo make some webui flag here
					polyLineManager.updatePolylines(getStatsSnapshot());
					ChartPackage.addChart(
							appConfig.getRunId(), LoadExecutorBase.this.getName(), polyLineManager);
				}
				try {
					TimeUnit.SECONDS.sleep(metricsPeriodSec);
				} catch(final InterruptedException e) {
					break;
				}
			}
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	protected LoadExecutorBase(
		final AppConfig appConfig,
		final IoConfig<? extends Item, ? extends Container<? extends Item>> ioConfig,
		final String addrs[], final int threadCount, final Input<T> itemInput,
		final long countLimit, final long sizeLimit, final float rateLimit,
		final int instanceNum, final String name
	) {
		super(
			itemInput, countLimit > 0 ? countLimit : Long.MAX_VALUE, DEFAULT_INTERNAL_BATCH_SIZE,
			appConfig.getLoadCircular(), false, appConfig.getItemQueueSizeLimit(), rateLimit
		);
		try {
			super.setOutput(this);
		} catch(final RemoteException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to set \"{}\" as a consumer of \"{}\" producer",
				name, itemInput
			);
		}
		itemOutBuff = new LimitedQueueItemBuffer<>(
			new ArrayBlockingQueue<T>(DEFAULT_RESULTS_QUEUE_SIZE)
		);
		//
		this.appConfig = appConfig;
		this.instanceNum = instanceNum;
		storageNodeCount = addrs == null ? 0 : addrs.length;
		//
		setName(name);
		if(itemInput != null) {
			LOG.info(Markers.MSG, "{}: will use \"{}\" as an item source", getName(), itemInput.toString());
		}
		//
		totalThreadCount = threadCount * storageNodeCount;
		activeTasksThrottle = new ActiveTasksThrottle<>(
			2 * totalThreadCount + 1000, counterSubm, counterResults, isInterrupted, isShutdown,
			isCircular
		);
		//
		IoConfig<? extends Item, ? extends Container<? extends Item>> reqConfigClone = null;
		try {
			reqConfigClone = ioConfig.clone();
		} catch(final CloneNotSupportedException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to clone the request config");
		} finally {
			this.ioConfigCopy = reqConfigClone;
		}
		loadType = ioConfig.getLoadType();
		//
		metricsPeriodSec = appConfig.getLoadMetricsPeriod();
		preconditionFlag = appConfig.getLoadPrecondition();
		this.sizeLimit = sizeLimit > 0 ? sizeLimit : Long.MAX_VALUE;
		// prepare the nodes array
		storageNodeAddrs = addrs == null ? null : addrs.clone();
		if(storageNodeAddrs != null) {
			nodeBalancer = new BasicNodeBalancer(storageNodeAddrs);
		}
		dataSrc = ioConfig.getContentSource();
		//
		mgmtExecutor = new ThreadPoolExecutor(
			1, 1, 0, TimeUnit.DAYS, new ArrayBlockingQueue<Runnable>(batchSize),
			new NamingThreadFactory("mgmtWorker", true)
		);
		if(metricsPeriodSec > 0) {
			mgmtTasks.add(new LogMetricsTask());
		}
		mgmtTasks.add(new StatsRefreshTask());
		mgmtTasks.add(new ResultsDispatcher());
		//
		LoadRegistry.register(this);
	}
	//
	private LoadExecutorBase(
		final AppConfig appConfig,
		final IoConfig<? extends DataItem, ? extends Container<? extends DataItem>> ioConfig,
		final String addrs[], final int threadCount, final Input<T> itemInput,
		final long countLimit, final long sizeLimit, final float rateLimit, final int instanceNum
	) {
		this(
			appConfig, ioConfig, addrs, threadCount, itemInput, countLimit, sizeLimit, rateLimit,
			instanceNum,
			instanceNum + "-" + ioConfig.toString() +
				(countLimit > 0 ? Long.toString(countLimit) : "") + '-' + threadCount +
				(addrs == null ? "" : 'x' + Integer.toString(addrs.length))
		);
	}
	//
	protected LoadExecutorBase(
		final AppConfig appConfig,
		final IoConfig<? extends DataItem, ? extends Container<? extends DataItem>> ioConfig,
		final String addrs[], final int threadCount, final Input<T> itemInput,
		final long countLimit, final long sizeLimit, final float rateLimit
	) {
		this(
			appConfig, ioConfig, addrs, threadCount, itemInput, countLimit, sizeLimit, rateLimit,
			NEXT_INSTANCE_NUM.getAndIncrement()
		);
	}
	//
	protected void initStats(final boolean flagServeJMX) {
		ioStats = new BasicIOStats(getName(), flagServeJMX, metricsPeriodSec);
		lastStats = ioStats.getSnapshot();
	}
	//
	@Override
	protected void runActually() {
		try {
			super.runActually();
		} finally {
			if(itemInput != null) {
				LOG.debug(
					Markers.MSG, "{}: scheduled {} tasks, invoking self-shutdown",
					getName(), counterSubm.get()
				);
				if(isShutdown.compareAndSet(false, true)) {
					shutdownActually();
				}
			}
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void logMetrics(final Marker logMarker) {
		if(preconditionFlag) {
			LOG.info(
				Markers.MSG,
				Markers.PERF_SUM.equals(logMarker) ?
					"\"" + getName() + "\" summary: " + lastStats.toSummaryString() :
					lastStats
			);
		} else {
			LOG.info(
				logMarker,
				Markers.PERF_SUM.equals(logMarker) ?
					"\"" + getName() + "\" summary: " + lastStats.toSummaryString() :
					lastStats
			);
		}
	}
	//
	@Override
	public final void start()
	throws IllegalStateException {
		if(isClosed.get()) {
			throw new IllegalStateException(getName() + ": closed already but start invoked");
		}
		if(isInterrupted.get()) {
			throw new IllegalStateException(getName() + ": interrupted already but start invoked");
		}
		if(isStarted.compareAndSet(false, true)) {
			startActually();
		} else {
			throw new IllegalStateException(getName() + ": was started already");
		}
	}
	//
	protected void startActually() {
		LOG.debug(Markers.MSG, "Starting {}", getName());
		initStats(appConfig.getNetworkServeJmx());
		ioStats.start();
		//
		if(appConfig.getRunResumeEnabled()) {
			if(appConfig.getRunMode().equals(Constants.RUN_MODE_STANDALONE)) {
				try {
					if(!RESTORED_STATES_MAP.containsKey(appConfig.getRunId())) {
						BasicLoadState.restoreScenarioState(appConfig);
					}
					setLoadState(BasicLoadState.<T>findStateByLoadNumber(instanceNum, appConfig));
				} catch (final Exception e) {
					LogUtil.exception(LOG, Level.ERROR, e, "Unexpected failure");
				}
			}
		}
		//
		refreshStats();
		if(isLimitReached) {
			try {
				close();
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Couldn't close the load executor \"{}\"", getName()
				);
			}
		} else {
			//
			if(counterResults.get() > 0) {
				setLastItem(loadedPrevState.getLastDataItem());
				setSkipCount(counterResults.get());
				skipIfNecessary();
			}
			super.start();
			LOG.debug(Markers.MSG, "Started object producer {}", getName());
			//
			mgmtExecutor.setCorePoolSize(mgmtTasks.size());
			mgmtExecutor.setMaximumPoolSize(mgmtTasks.size());
			for(final Runnable mgmtTask : mgmtTasks) {
				mgmtExecutor.submit(mgmtTask);
			}
			mgmtExecutor.shutdown();
			//
			LOG.debug(Markers.MSG, "Started \"{}\"", getName());
		}
	}
	//
	@Override
	public final void interrupt() {
		if(isStarted.get()) {
			if(isInterrupted.compareAndSet(false, true)) {
				synchronized(state) {
					state.notifyAll();
				}
				interruptActually();
			}
		} else {
			throw new IllegalStateException(
				getName() + ": not started yet but interrupt is invoked"
			);
		}
	}
	//
	protected void interruptActually() {
		if(LOG.isTraceEnabled(Markers.MSG)) {
			final StringBuilder sb = new StringBuilder("Interrupt came from:");
			final StackTraceElement stackTrace[] = Thread.currentThread().getStackTrace();
			for(final StackTraceElement ste : stackTrace) {
				sb.append("\n\t").append(ste.toString());
			}
			LOG.trace(Markers.MSG, sb);
		}
		super.interrupt();
		if(isShutdown.compareAndSet(false, true)) {
			shutdownActually();
		}
		try {
			ioConfigCopy.close(); // disables connection drop failures
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to close the request configurator");
		}
		//
		LOG.debug(Markers.MSG, "{}: waiting the output buffer to become empty", getName());
		for(int i = 0; i < 1000 && !itemOutBuff.isEmpty(); i ++) {
			try {
				Thread.sleep(10);
			} catch(final InterruptedException e) {
				break;
			}
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
					eff = lastStats.getDurationSum() / loadDurMicroSec / totalThreadCount;
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
		mgmtExecutor.shutdownNow();
		LOG.debug(Markers.MSG, "{}: service threads executor shut down", getName());
		//
		if(isCircular) {
			final List<T> items = Collections.list(Collections.enumeration(uniqueItems.values()));
			postProcessUniqueItemsFinally(items);
		}
		uniqueItems.clear();
		//
		if(consumer instanceof LifeCycle) {
			try {
				((LifeCycle) consumer).shutdown();
				LOG.debug(
					Markers.MSG, "{}: shut down the consumer \"{}\" successfully",
					getName(), consumer
				);
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "{}: failed to shut down the consumer \"{}\"",
					getName(), consumer
				);
			}
		}
		super.interrupt();
		//
		LOG.debug(Markers.MSG, "{} interrupted", getName());
	}
	//
	@Override
	public void setOutput(final Output<T> itemOutput)
	throws RemoteException {
		this.consumer = itemOutput;
		LOG.debug(Markers.MSG, getName() + ": appended the consumer \"" + itemOutput + "\"");
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Consumer implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void put(final T item)
	throws IOException {
		if(counterSubm.get() + countRej.get() >= countLimit) {
			LOG.debug(
				Markers.MSG, "{}: all tasks has been submitted ({}) or rejected ({})", getName(),
				counterSubm.get(), countRej.get()
			);
			if(isShutdown.compareAndSet(false, true)) {
				shutdownActually();
			}
			return;
		}
		// prepare the I/O task instance (make the link between the data item and load type)
		final String nextNodeAddr = storageNodeAddrs == null ?
			null : storageNodeCount == 1 ? storageNodeAddrs[0] : nodeBalancer.getNext();
		final IOTask<T> ioTask = getIOTask(item, nextNodeAddr);
		// don't fill the connection pool as fast as possible, this may cause a failure
		//
		try {
			if(!activeTasksThrottle.requestContinueFor(item) || null == submitTask(ioTask)) {
				throw new RejectedExecutionException();
			}
			counterSubm.incrementAndGet();
			if(nodeBalancer != null) {
				nodeBalancer.markTaskStart(nextNodeAddr);
			}
		} catch(final InterruptedException | RejectedExecutionException e) {
			if(!isInterrupted.get()) {
				countRej.incrementAndGet();
				LogUtil.exception(LOG, Level.DEBUG, e, "Rejected the I/O task {}", ioTask);
			}
		}
	}
	//
	@Override
	public int put(final List<T> srcBuff, final int from, final int to)
	throws IOException {
		final long dstLimit = countLimit - counterSubm.get() - countRej.get();
		final int srcLimit = to - from;
		int n = 0, m;
		if(dstLimit > 0) {
			if(dstLimit < srcLimit) {
				return put(srcBuff, from, from + (int) dstLimit);
			} else {
				// select the target node
				final String nextNodeAddr = storageNodeAddrs == null ?
					null : storageNodeCount == 1 ? storageNodeAddrs[0] : nodeBalancer.getNext();
				// prepare the I/O tasks list (make the link between the data item and load type)
				final List<IOTask<T>> ioTaskBuff = new ArrayList<>(srcLimit);
				getIOTasks(srcBuff, from, to, ioTaskBuff, nextNodeAddr);
				// submit all I/O tasks
				while(n < srcLimit) {
					// don't fill the connection pool as fast as possible, this may cause a failure
					try {
						activeTasksThrottle.requestContinueFor(null, to - from);
						m = submitTasks(ioTaskBuff, n, srcLimit);
						if(m < 1) {
							throw new RejectedExecutionException("No I/O tasks submitted");
						} else {
							n += m;
						}
						counterSubm.addAndGet(m);
						// increment node's usage counter
						if(nodeBalancer != null) {
							nodeBalancer.markTasksStart(nextNodeAddr, m);
						}
					} catch(final InterruptedException | RejectedExecutionException e) {
						if(isInterrupted.get()) {
							throw new InterruptedIOException(getName() + " is interrupted");
						} else {
							m = srcLimit - n;
							countRej.addAndGet(m);
							LogUtil.exception(LOG, Level.DEBUG, e, "Rejected {} I/O tasks", m);
						}
					}
				}
			}
		} else {
			if(isShutdown.compareAndSet(false, true)) {
				shutdownActually();
			}
			if(srcLimit > 0) {
				countRej.addAndGet(srcLimit);
				LOG.debug(Markers.MSG, "Rejected {} I/O tasks", srcLimit);
			}
		}
		return n;
	}
	//
	@Override
	public final int put(final List<T> items)
	throws IOException {
		return put(items, 0, items.size());
	}
	//
	protected abstract IOTask<T> getIOTask(final T item, final String nextNodeAddr);
	//
	protected int getIOTasks(
		final List<T> items, final int from, final int to,
		final List<IOTask<T>> dstTaskBuff, final String nextNodeAddr
	) {
		for(final T item : items) {
			if(item == null) {
				break;
			} else {
				dstTaskBuff.add(getIOTask(item, nextNodeAddr));
			}
		}
		return to - from;
	}
	//
	private final static ThreadLocal<StringBuilder>
		PERF_TRACE_MSG_BUILDER = new ThreadLocal<StringBuilder>() {
		@Override
		protected final StringBuilder initialValue() {
			return new StringBuilder();
		}
	};
	private void logTrace(
		final String nodeAddr, final String itemName, final IOTask.Status status,
		final long reqTimeStart, final long countBytesDone, final int reqDuration,
		final int respLatency, final long respDataLatency
	) {
		if(LOG.isInfoEnabled(Markers.PERF_TRACE)) {
			StringBuilder strBuilder = PERF_TRACE_MSG_BUILDER.get();
			if(strBuilder == null) {
				strBuilder = new StringBuilder();
				PERF_TRACE_MSG_BUILDER.set(strBuilder);
			} else {
				strBuilder.setLength(0); // clear/reset
			}
			LOG.info(
				Markers.PERF_TRACE,
				strBuilder
					//.append(loadType).append(',')
					.append(nodeAddr == null ? "" : nodeAddr).append(',')
					.append(itemName).append(',')
					.append(countBytesDone).append(',')
					.append(status.code).append(',')
					.append(reqTimeStart).append(',')
					.append(respLatency > 0 ? respLatency : 0).append(',')
					.append(respDataLatency).append(',')
					.append(reqDuration)
					.toString()
			);
		}
	}
	//
	@Override
	public void ioTaskCompleted(final IOTask<T> ioTask)
	throws RemoteException {
		// producing was interrupted?
		if(isInterrupted.get()) {
			return;
		}
		//
		final T item = ioTask.getItem();
		final IOTask.Status status = ioTask.getStatus();
		final String nodeAddr = ioTask.getNodeAddr();
		final int
			reqDuration = ioTask.getDuration(),
			respLatency = ioTask.getLatency(),
			respDataLatency = ioTask.getDataLatency();
		final long countBytesDone = ioTask.getCountBytesDone();
		// perf trace logging
		if(!preconditionFlag && !(this instanceof MixedLoadExecutor)) {
			logTrace(
				nodeAddr, item.getName(), status, ioTask.getReqTimeStart(), countBytesDone,
				reqDuration, respLatency, respDataLatency
			);
		}
		//
		if(nodeBalancer != null) {
			nodeBalancer.markTaskFinish(nodeAddr);
		}
		if(IOTask.Status.SUCC == status) {
			// update the metrics with success
			if(respLatency > 0 && respLatency > reqDuration) {
				LOG.warn(
					Markers.ERR, "{}: latency {} is more than duration: {}", this, respLatency,
					reqDuration
				);
			}
			ioStats.markSucc(countBytesDone, reqDuration, respLatency);
			//
			lastItem = item;
			// put into the output buffer
			try {
				itemOutBuff.put(item);
				if(isCircular) {
					uniqueItems.putIfAbsent(item.getName(), item);
				}
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.DEBUG, e,
					"{}: failed to put the item into the output buffer", getName()
				);
			}
		} else {
			ioStats.markFail();
		}
		//
		counterResults.incrementAndGet();
	}
	//
	@Override
	public int ioTaskCompletedBatch(
		final List<? extends IOTask<T>> ioTasks, final int from, final int to
	) throws RemoteException {
		// producing was interrupted?
		if(isInterrupted.get()) {
			return 0;
		}
		//
		final int n = to - from;
		if(n > 0) {
			if(storageNodeAddrs != null) {
				final String nodeAddr = ioTasks.get(from).getNodeAddr();
				nodeBalancer.markTasksFinish(nodeAddr, n);
			}
			//
			IOTask<T> ioTask;
			T item;
			IOTask.Status status;
			String nodeAddr;
			int reqDuration, respLatency, respDataLatency;
			long countBytesDone;
			for(int i = from; i < to; i++) {
				//
				ioTask = ioTasks.get(i);
				item = ioTask.getItem();
				status = ioTask.getStatus();
				nodeAddr = ioTask.getNodeAddr();
				reqDuration = ioTask.getDuration();
				respLatency = ioTask.getLatency();
				respDataLatency = ioTask.getDataLatency();
				countBytesDone = ioTask.getCountBytesDone();
				// perf trace logging
				if(!preconditionFlag) {
					logTrace(
						nodeAddr, item.getName(), status, ioTask.getReqTimeStart(), countBytesDone,
						reqDuration, respLatency, respDataLatency
					);
				}
				//
				if(IOTask.Status.SUCC == status) {
					// update the metrics with success
					if(respLatency > 0 && respLatency > reqDuration) {
						LOG.warn(
							Markers.ERR, "{}: latency {} is more than duration: {}", this, respLatency,
							reqDuration
						);
					}
					ioStats.markSucc(countBytesDone, reqDuration, respLatency);
					//
					lastItem = item;
					// pass data item to a consumer
					try {
						itemOutBuff.put(item);
						if(isCircular) {
							uniqueItems.putIfAbsent(item.getName(), item);
						}
					} catch(final IOException e) {
						LogUtil.exception(
							LOG, Level.DEBUG, e,
							"{}: failed to put the item into the output buffer", getName()
						);
					}
				} else {
					ioStats.markFail();
				}
			}
			synchronized(ioStats) {
				ioStats.notifyAll();
			}
			counterResults.addAndGet(n);
		}
		//
		return n;
	}
	//
	protected void ioTaskCancelled(final int n) {
		LOG.debug(Markers.MSG, "{}: I/O task canceled", hashCode());
		ioStats.markFail(n);
		counterResults.addAndGet(n);
	}
	//
	protected void ioTaskFailed(final int n, final Throwable e) {
		ioStats.markFail(n);
		counterResults.addAndGet(n);
	}
	//
	protected void postProcessItems()
	throws InterruptedException {
		try {
			//
			final List<T> items = new ArrayList<>(batchSize);
			final int n = itemOutBuff.get(items, batchSize);
			if(n > 0) {
				if(isCircular) {
					int m = 0, k;
					while(m < n) {
						k = put(items, m, n);
						if(k > 0) {
							m += k;
						} else {
							break;
						}
						LockSupport.parkNanos(1_000);
					}
				} else {
					postProcessUniqueItemsFinally(items);
				}
			}
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.DEBUG, e, "Failed to feed the items to \"{}\"", consumer
			);
		} catch(final RejectedExecutionException e) {
			if(LOG.isTraceEnabled(Markers.ERR)) {
				LogUtil.exception(LOG, Level.TRACE, e, "\"{}\" rejected the items", consumer);
			}
		}
	}
	//
	protected void postProcessUniqueItemsFinally(final List<T> items) {
		// is this an end of consumer-producer chain?
		if(consumer == null) {
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "{}: going to dump out {} items", getName(), items.size());
			}
			if(LOG.isInfoEnabled(Markers.ITEM_LIST)) {
				try {
					for(final Item item : items) {
						LOG.info(Markers.ITEM_LIST, item);
					}
				} catch(final Throwable e) {
					LogUtil.exception(
						LOG, Level.ERROR, e, "{}: failed to dump out {} items",
						getName(), items.size()
					);
				}
			}
		} else { // put to the consumer
			int n = items.size();
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Going to put {} items to the consumer {}",
					n, consumer
				);
			}
			try {
				if(!items.isEmpty()) {
					for(int m = 0; m < n; m += consumer.put(items, m, n)) {
						LockSupport.parkNanos(1);
					}
				}
				items.clear();
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.DEBUG, e, "Failed to feed the items to \"{}\"", consumer
				);
			}
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG,
					"{} items were passed to the consumer {} successfully",
					n, consumer
				);
			}
		}
	}
	//
	private void refreshStats() {
		lastStats = ioStats.getSnapshot();
	}
	//
	protected void checkForBadState()
	throws InterruptedException {
		if(
			lastStats.getFailCount() > MAX_FAIL_COUNT &&
			lastStats.getFailRateLast() > lastStats.getSuccRateLast()
		) {
			LOG.fatal(
				Markers.ERR,
				"There's a more than {} of failures and the failure rate is higher " +
					"than success rate for at least last {}[sec]. Exiting in order to " +
					"avoid the memory exhaustion. Please check your environment.",
				MAX_FAIL_COUNT, metricsPeriodSec
			);
			try {
				interrupt();
				throw new InterruptedException();
			} finally {
				Thread.currentThread().interrupt();
			}
		}
	}
	//
	@Override
	public void setLoadState(final LoadState<T> state) {
		if(state != null) {
			if(state.isLimitReached(appConfig)) {
				isLimitReached = true;
				LOG.warn(Markers.MSG, "\"{}\": nothing to do more", getName());
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
			synchronized(this.state) {
				state.notifyAll();
			}
			synchronized(ioStats) {
				ioStats.notifyAll();
			}
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public LoadState<T> getLoadState()
	throws RemoteException {
		return new BasicLoadState.Builder<T, BasicLoadState<T>>()
			.setLoadNumber(instanceNum)
			.setAppConfig(appConfig)
			.setStatsSnapshot(lastStats)
			.setLastDataItem(lastItem)
			.build();
	}
	//
	@Override
	public IOStats.Snapshot getStatsSnapshot() {
		return lastStats;
	}
	//
	private boolean isDoneCountLimit() {
		return counterResults.get() >= countLimit;
	}
	//
	private boolean isDoneSizeLimit() {
		return lastStats.getByteCount() >= sizeLimit;
	}
	//
	private boolean isDoneAllSubm() {
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, "{}: shut down flag: {}, results: {}, submitted: {}",
				getName(), isShutdown.get(), counterResults.get(), counterSubm.get()
			);
		}
		return isShutdown.get() && counterResults.get() >= counterSubm.get();
	}
	//
	@Override
	public final void shutdown()
	throws IllegalStateException {
		if(isStarted.get()) {
			if(isShutdown.compareAndSet(false, true)) {
				synchronized(state) {
					state.notifyAll();
				}
				shutdownActually();
			}
		} else {
			throw new IllegalStateException(
				getName() + ": not started yet but shutdown is invoked"
			);
		}
	}
	//
	protected void shutdownActually() {
		super.interrupt(); // stop the source producing right now
		if(isCircular) {
			allItemsProducedFlag = true; //  unblock ResultsDispatcher thread
		}
		LOG.debug(Markers.MSG, "Stopped the producing from \"{}\" for \"{}\"", itemInput, getName());
	}
	//
	@Override
	public final void await()
	throws InterruptedException, RemoteException {
		await(Long.MAX_VALUE, TimeUnit.DAYS);
	}
	//
	@Override
	public void await(final long timeOut, final TimeUnit timeUnit)
	throws InterruptedException, RemoteException {
		long t, timeOutNanoSec = timeUnit.toNanos(timeOut);
		if(loadedPrevState != null) {
			if(isLimitReached) {
				return;
			}
			t = TimeUnit.MICROSECONDS.toNanos(
				loadedPrevState.getStatsSnapshot().getElapsedTime()
			);
			timeOutNanoSec -= t;
		}
		//
		LOG.debug(
			Markers.MSG, "{}: await for the done condition at most for {}[s]",
			getName(), TimeUnit.NANOSECONDS.toSeconds(timeOutNanoSec)
		);
		t = System.nanoTime();
		while(true) {
			synchronized(state) {
				state.wait(100);
			}
			if(isInterrupted.get()) {
				LOG.debug(Markers.MSG, "{}: await exit due to interrupted state", getName());
				break;
			}
			if(isClosed.get()) {
				LOG.debug(Markers.MSG, "{}: await exit due to closed state", getName());
				break;
			}
			if(isDoneAllSubm()) {
				if(!isCircular) {
					LOG.debug(Markers.MSG, "{}: await exit due to \"done all submitted\" state", getName());
					break;
				}
			}
			if(isDoneCountLimit()) {
				LOG.debug(Markers.MSG, "{}: await exit due to max count done state", getName());
				break;
			}
			if(System.nanoTime() - t > timeOutNanoSec) {
				LOG.debug(Markers.MSG, "{}: await exit due to timeout", getName());
				break;
			}
			if(isLimitReached) {
				LOG.debug(Markers.MSG, "{}: await exit due to limits reached state", getName());
				break;
			}
			LockSupport.parkNanos(1_000_000);
		}
	}
	//
	@Override
	public final void close()
	throws IOException, IllegalStateException {
		if(isClosed.compareAndSet(false, true)) {
			synchronized(state) {
				state.notifyAll();
			}
			closeActually();
		}
	}
	//
	protected void closeActually()
	throws IOException {
		LOG.debug(Markers.MSG, "Invoked close for {}", getName());
		try {
			if(isInterrupted.compareAndSet(false, true)) {
				synchronized(state) {
					state.notifyAll();
				}
				interruptActually();
			}
		} finally {
			if(consumer != null && !(consumer instanceof LifeCycle)) {
				try {
					//
					consumer.close();
					LOG.debug(
						Markers.MSG, "{}: closed the consumer \"{}\" successfully",
						getName(), consumer
					);
				} catch(final IOException e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "{}: failed to close the consumer \"{}\"",
						getName(), consumer
					);
				}
			}
			LoadRegistry.unregister(this);
			if(loadedPrevState != null) {
				if(RESTORED_STATES_MAP.containsKey(appConfig.getRunId())) {
					RESTORED_STATES_MAP.get(appConfig.getRunId()).remove(loadedPrevState);
				}
			}
		}
		LOG.debug(Markers.MSG, "\"{}\" closed successfully", getName());
	}
	//
	@Override
	protected final void finalize()
	throws Throwable {
		try {
			if(!isClosed.get()) {
				close();
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "{}: failed to close", getName());
		} finally {
			super.finalize();
		}
	}
	//
	@Override
	public final String toString() {
		return getName();
	}
}
