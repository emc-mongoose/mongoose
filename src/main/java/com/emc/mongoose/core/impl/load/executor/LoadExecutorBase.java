package com.emc.mongoose.core.impl.load.executor;
// mongoose-common.jar
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.concurrent.LifeCycle;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.base.ItemDst;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.item.base.ItemBuffer;
import com.emc.mongoose.core.api.io.conf.IOConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.item.data.ContentSource;
import com.emc.mongoose.core.api.load.balancer.Balancer;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.model.metrics.IOStats;
import com.emc.mongoose.core.api.load.model.LoadState;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.item.base.LimitedQueueItemBuffer;
import com.emc.mongoose.core.impl.load.balancer.BasicNodeBalancer;
import com.emc.mongoose.core.impl.load.model.metrics.BasicIOStats;
import com.emc.mongoose.core.impl.load.tasks.LoadCloseHook;
import com.emc.mongoose.core.impl.load.model.BasicLoadState;
import com.emc.mongoose.core.impl.load.model.BasicItemProducer;
//
import org.apache.logging.log4j.Level;
import com.emc.mongoose.core.impl.load.tasks.LogMetricsTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
//
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
/**
 Created by kurila on 15.10.14.
 */
public abstract class LoadExecutorBase<T extends Item>
extends BasicItemProducer<T>
implements LoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final int instanceNum, storageNodeCount;
	protected final String storageNodeAddrs[];
	//
	protected final RunTimeConfig rtConfig;
	//
	protected final ContentSource dataSrc;
	protected final IOConfig<? extends DataItem, ? extends Container<? extends DataItem>>
		ioConfigCopy;
	protected final IOTask.Type loadType;
	//
	protected volatile ItemDst<T> consumer = null;
	//
	protected final long maxCount;
	protected final int totalConnCount, activeTaskCountLimit;
	// METRICS section
	protected final int metricsPeriodSec;
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
	private final class StatsRefreshTask
	implements Runnable {
		@Override
		public final void run() {
			final Thread currThread = Thread.currentThread();
			currThread.setName("statsRefresh<" + getName() + ">");
			try {
				while(!currThread.isInterrupted()) {
					synchronized(ioStats) {
						ioStats.wait(1000);
					}
					refreshStats();
					Thread.yield();
					LockSupport.parkNanos(1000000);
				}
			} catch(final InterruptedException ignored) {
			}
		}
	}
	//
	public final static int MAX_FAIL_COUNT = 100000;
	private final class FailuresMonitorTask
	implements Runnable {
		@Override
		public final void run() {
			final Thread currThread = Thread.currentThread();
			currThread.setName("failuresMonitor<" + getName() + ">");
			try {
				while(!currThread.isInterrupted()) {
					synchronized(ioStats) {
						ioStats.wait(1000);
					}
					checkForBadState();
					Thread.yield();
					LockSupport.parkNanos(1000000);
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
					while(!areAllItemsProduced) {
						LockSupport.parkNanos(1000000);
						Thread.yield();
					}
				}
				while(!currThread.isInterrupted()) {
					passItems();
					Thread.yield();
					LockSupport.parkNanos(1000);
				}
			} catch(final InterruptedException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Interrupted");
			} finally {
				LOG.debug(Markers.MSG, "{}: results dispatched finished", getName());
			}
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	protected LoadExecutorBase(
		final RunTimeConfig rtConfig,
		final IOConfig<? extends DataItem, ? extends Container<? extends DataItem>> ioConfig,
		final String addrs[], final int connCountPerNode, final int threadCount,
		final ItemSrc<T> itemSrc, final long maxCount,
		final int instanceNum, final String name
	) {
		super(
			itemSrc, maxCount > 0 ? maxCount : Long.MAX_VALUE,
			DEFAULT_INTERNAL_BATCH_SIZE, rtConfig.isLoadCircular(),
			rtConfig.isShuffleItemsEnabled(), rtConfig.getItemQueueMaxSize()
		);
		try {
			super.setItemDst(this);
		} catch(final RemoteException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to set \"{}\" as a consumer of \"{}\" producer",
				name, itemSrc
			);
		}
		itemOutBuff = new LimitedQueueItemBuffer<>(
			new ArrayBlockingQueue<T>(DEFAULT_RESULTS_QUEUE_SIZE)
		);
		//
		this.rtConfig = rtConfig;
		this.instanceNum = instanceNum;
		storageNodeCount = addrs == null ? 0 : addrs.length;
		//
		setName(name);
		if(itemSrc != null) {
			LOG.info(Markers.MSG, "{}: will use \"{}\" as an item source", getName(), itemSrc.toString());
		}
		//
		totalConnCount = connCountPerNode * storageNodeCount;
		activeTaskCountLimit = 2 * totalConnCount + 1000;
		//
		IOConfig<? extends DataItem, ? extends Container<? extends DataItem>> reqConfigClone = null;
		try {
			reqConfigClone = ioConfig.clone();
		} catch(final CloneNotSupportedException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to clone the request config");
		} finally {
			this.ioConfigCopy = reqConfigClone;
		}
		loadType = ioConfig.getLoadType();
		//
		metricsPeriodSec = rtConfig.getLoadMetricsPeriodSec();
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		// prepare the nodes array
		storageNodeAddrs = addrs == null ? null : addrs.clone();
		if(storageNodeAddrs != null) {
			nodeBalancer = new BasicNodeBalancer(storageNodeAddrs);
		}
		dataSrc = ioConfig.getContentSource();
		//
		mgmtExecutor = new ThreadPoolExecutor(
			1, 1, 0, TimeUnit.DAYS, new ArrayBlockingQueue<Runnable>(batchSize),
			new GroupThreadFactory("mgmtWorker", true)
		);
		if(metricsPeriodSec > 0) {
			mgmtTasks.add(new LogMetricsTask(this, metricsPeriodSec));
		}
		mgmtTasks.add(new StatsRefreshTask());
		mgmtTasks.add(new FailuresMonitorTask());
		mgmtTasks.add(new ResultsDispatcher());
		//
		LoadCloseHook.add(this);
	}
	//
	private LoadExecutorBase(
		final RunTimeConfig rtConfig,
		final IOConfig<? extends DataItem, ? extends Container<? extends DataItem>> ioConfig,
		final String addrs[], final int connCountPerNode, final int threadCount,
		final ItemSrc<T> itemSrc, final long maxCount, final int instanceNum
	) {
		this(
			rtConfig, ioConfig, addrs, connCountPerNode, threadCount, itemSrc, maxCount,
			instanceNum,
			Integer.toString(instanceNum) + '-' + ioConfig.toString() +
				(maxCount > 0 ? Long.toString(maxCount) : "") + '-' +
				Integer.toString(connCountPerNode > 0 ? connCountPerNode : threadCount) +
				(addrs == null ? "" : 'x' + Integer.toString(addrs.length))
		);
	}
	//
	protected LoadExecutorBase(
		final RunTimeConfig rtConfig,
		final IOConfig<? extends DataItem, ? extends Container<? extends DataItem>> ioConfig,
		final String addrs[], final int connCountPerNode, final int threadCount,
		final ItemSrc<T> itemSrc, final long maxCount
	) {
		this(
			rtConfig, ioConfig, addrs, connCountPerNode, threadCount, itemSrc, maxCount,
			NEXT_INSTANCE_NUM.getAndIncrement()
		);
	}
	//
	protected void initStats(final boolean flagServeJMX) {
		if(flagServeJMX) {
			ioStats = new BasicIOStats(
				getName(), rtConfig.getRemotePortMonitor(), metricsPeriodSec
			);
		} else {
			ioStats = new BasicIOStats(getName(), 0, metricsPeriodSec);
		}
		lastStats = ioStats.getSnapshot();
	}
	//
	@Override
	protected void runActually() {
		try {
			super.runActually();
		} finally {
			if(itemSrc != null) {
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
		initStats(rtConfig.getFlagServeJMX());
		ioStats.start();
		//
		if(rtConfig.isRunResumeEnabled()) {
			if(rtConfig.getRunMode().equals(Constants.RUN_MODE_STANDALONE)) {
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
				LOG.info(Markers.PERF_SUM, "\"" + getName() + "\" summary: " + lastStats.toSummaryString());
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
		mgmtExecutor.shutdownNow();
		LOG.debug(Markers.MSG, "{}: service threads executor shut down", getName());
		//
		if(isCircular) {
			final List<T> itemsList = Collections.list(
				Collections.enumeration(uniqueItems.values())
			);
			passUniqueItemsFinally(itemsList);
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
	public void setItemDst(final ItemDst<T> itemDst)
	throws RemoteException {
		this.consumer = itemDst;
		LOG.debug(Markers.MSG, getName() + ": appended the consumer \"" + itemDst + "\"");
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Consumer implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void put(final T dataItem)
	throws IOException {
		if(counterSubm.get() + countRej.get() >= maxCount) {
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
		final IOTask<T> ioTask = getIOTask(dataItem, nextNodeAddr);
		// don't fill the connection pool as fast as possible, this may cause a failure
		int activeTaskCount;
		do {
			activeTaskCount = (int) (counterSubm.get() - counterResults.get());
			if(isInterrupted.get() || (isShutdown.get() && !isCircular)) {
				throw new InterruptedIOException(
					getName() + ": submit failed, shut down already or interrupted"
				);
			}
			if(activeTaskCount < activeTaskCountLimit) {
				break;
			}
			LockSupport.parkNanos(1000); Thread.yield();
		} while(true);
		//
		try {
			if(null == submitReq(ioTask)) {
				throw new RejectedExecutionException("Null future returned");
			}
			counterSubm.incrementAndGet();
			if(nodeBalancer != null) {
				nodeBalancer.markTaskStart(nextNodeAddr);
			}
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
	throws IOException {
		final long dstLimit = maxCount - counterSubm.get() - countRej.get();
		final int srcLimit = to - from;
		int n = 0, m, activeTaskCount;
		if(dstLimit > 0) {
			if(dstLimit < srcLimit) {
				return put(srcBuff, from, from + (int) dstLimit);
			} else {
				// select the target node
				final String nextNodeAddr = storageNodeAddrs == null ?
					null : storageNodeCount == 1 ? storageNodeAddrs[0] : nodeBalancer.getNext();
				// prepare the I/O tasks list (make the link between the data item and load type)
				final List<IOTask<T>> ioTaskBuff = new ArrayList<>(srcLimit);
				if(srcLimit > getIOTasks(srcBuff, from, to, ioTaskBuff, nextNodeAddr)) {
					LOG.warn(Markers.ERR, "Produced less I/O tasks then expected ({})", srcLimit);
				}
				// submit all I/O tasks
				while(n < srcLimit) {
					// don't fill the connection pool as fast as possible, this may cause a failure
					do {
						activeTaskCount = (int) (counterSubm.get() - counterResults.get());
						if(isInterrupted.get() || (isShutdown.get() && !isCircular)) {
							throw new InterruptedIOException(
								getName() + ": submit failed, shut down already or interrupted"
							);
						}
						if(activeTaskCount < activeTaskCountLimit) {
							break;
						}
						LockSupport.parkNanos(1000); Thread.yield();
					} while(true);
					//
					try {
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
					} catch(final RejectedExecutionException e) {
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
	protected final void ioTaskCompleted(final IOTask<T> ioTask) {
		// producing was interrupted?
		if(isInterrupted.get()) {
			return;
		}
		//
		final T item = ioTask.getItem();
		//
		final IOTask.Status status = ioTask.getStatus();
		final String nodeAddr = ioTask.getNodeAddr();
		// update the metrics
		ioTask.mark(ioStats);
		if(nodeBalancer != null) {
			nodeBalancer.markTaskFinish(nodeAddr);
		}
		if(status == IOTask.Status.SUCC) {
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
	protected final int ioTaskCompletedBatch(
		final List<? extends IOTask<T>> ioTasks, final int from, final int to
	) {
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
			for(int i = from; i < to; i++) {
				ioTask = ioTasks.get(i);
				item = ioTask.getItem();
				//
				status = ioTask.getStatus();
				// update the metrics
				ioTask.mark(ioStats);
				if(status == IOTask.Status.SUCC) {
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
	protected final void ioTaskCancelled(final int n) {
		LOG.debug(Markers.MSG, "{}: I/O task canceled", hashCode());
		ioStats.markFail(n);
		counterResults.addAndGet(n);
	}
	//
	protected final void ioTaskFailed(final int n, final Throwable e) {
		ioStats.markFail(n);
		counterResults.addAndGet(n);
		if(!isClosed.get() && !isInterrupted.get()) {
			LogUtil.exception(LOG, Level.DEBUG, e, "{}: I/O tasks ({}) failure", getName(), n);
		} else {
			LOG.debug(Markers.ERR, "{}: {} I/O tasks has been interrupted", getName(), n);
		}
	}
	//
	protected void passItems()
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
						Thread.yield(); LockSupport.parkNanos(1);
					}
				} else {
					passUniqueItemsFinally(items);
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
	protected void passUniqueItemsFinally(final List<T> items) {
		// is this an end of consumer-producer chain?
		if(consumer == null) {
			LOG.debug(Markers.MSG, "{}: going to dump out {} items", getName(), items.size());
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
					int m = 0, k;
					while(m < n) {
						k = consumer.put(items, m, n);
						if(k > 0) {
							m += k;
						}
						Thread.yield();
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
	protected void checkForBadState() {
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
			} catch(final Exception e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to interrupt the load job");
			} finally {
				Thread.currentThread().interrupt();
			}
		}
	}
	//
	@Override
	public void setLoadState(final LoadState<T> state) {
		if(state != null) {
			if(state.isLimitReached(rtConfig)) {
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
			.setRunTimeConfig(rtConfig)
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
	private boolean isDoneMaxCount() {
		return counterResults.get() >= maxCount;
	}
	//
	private void setCountLimitConfig(final long itemsCount) {
		if(isDoneAllSubm() && (maxCount > itemsCount)) {
			rtConfig.set(RunTimeConfig.KEY_DATA_ITEM_COUNT, itemsCount);
		} else {
			rtConfig.set(RunTimeConfig.KEY_DATA_ITEM_COUNT, maxCount);
		}
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
			areAllItemsProduced = true; //  unblock ResultsDispatcher thread
		}
		LOG.debug(Markers.MSG, "Stopped the producing from \"{}\" for \"{}\"", itemSrc, getName());
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
			if(isDoneMaxCount()) {
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
			Thread.yield(); LockSupport.parkNanos(1000000);
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
			setCountLimitConfig(counterResults.get());
			LoadCloseHook.del(this);
			if(loadedPrevState != null) {
				if(RESTORED_STATES_MAP.containsKey(rtConfig.getRunId())) {
					RESTORED_STATES_MAP.get(rtConfig.getRunId()).remove(loadedPrevState);
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
