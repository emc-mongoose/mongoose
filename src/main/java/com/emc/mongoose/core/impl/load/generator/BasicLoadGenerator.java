package com.emc.mongoose.core.impl.load.generator;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.io.task.IoTask;
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.base.ItemBuffer;
import com.emc.mongoose.core.api.load.barrier.Barrier;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.generator.LoadGenerator;
import com.emc.mongoose.core.api.load.generator.LoadState;
import com.emc.mongoose.core.api.load.metrics.IoStats;
//
import com.emc.mongoose.core.impl.io.task.BasicIoTask;
import com.emc.mongoose.core.impl.item.base.LimitedQueueItemBuffer;
import com.emc.mongoose.core.impl.load.barrier.RateLimitBarrier;
import com.emc.mongoose.core.impl.load.metrics.BasicIoStats;
//
import com.emc.mongoose.core.impl.load.tasks.LoadCloseHook;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
//
import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 Created by kurila on 19.06.15.
 */
public class BasicLoadGenerator<T extends Item, A extends IoTask<T>>
extends Thread
implements LoadGenerator<T, A> {
	//
	private final static Logger LOG = LogManager.getLogger();
	// immutable properties
	protected final LoadType loadType;
	private final LoadExecutor<T, A> executor;
	private final Input<T> itemInput;
	private final long countLimit;
	private final int weight;
	private final boolean circularFlag;
	private final boolean shuffleFlag;
	private final LoadState<T> loadState;
	// mutable properties
	private volatile Output<T> itemOutput = null;
	// other properties
	private final AppConfig appConfig;
	private final long skipCount;
	private final Map<String, T> uniqueItems;
	private final Barrier<T> rateLimitBarrier;
	private final int batchSize;
	private final int itemQueueSizeLimit;
	protected final ItemBuffer<T> itemOutBuff;
	// internal states
	private final IoStats ioStats;
	private volatile T lastItem = null;
	private volatile IoStats.Snapshot lastStats;
	private final AtomicLong counterSubm = new AtomicLong(0);
	private final AtomicLong counterResults = new AtomicLong(0);
	private volatile boolean allItemsProducedFlag = false;
	private volatile boolean isLimitReached = false;
	private final AtomicBoolean isStarted = new AtomicBoolean(false);
	private final AtomicBoolean isShutdown = new AtomicBoolean(false);
	private final AtomicBoolean isInterrupted = new AtomicBoolean(false);
	private final AtomicBoolean isClosed = new AtomicBoolean(false);
	//
	public BasicLoadGenerator(
		final AppConfig appConfig, final String name, final LoadType loadType,
		final LoadExecutor<T, A> executor, final Input<T> itemInput, final long countLimit,
		final int weight, final float rateLimit, final boolean isCircular, final boolean shuffleFlag
	) {
		this(
			appConfig, name, loadType, executor, itemInput, countLimit, weight, rateLimit,
			isCircular, shuffleFlag, null
		);
	}
	//
	public BasicLoadGenerator(
		final AppConfig appConfig, final String name, final LoadType loadType,
		final LoadExecutor<T, A> executor, final Input<T> itemInput, final long countLimit,
		final int weight, final float rateLimit, final boolean isCircular,
		final boolean shuffleFlag, final LoadState<T> loadState
	) throws IllegalStateException {
		this.appConfig = appConfig;
		if(name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Empty load job name");
		}
		setName(name);
		if(loadType == null) {
			throw new IllegalArgumentException("Load type is null");
		}
		this.loadType = loadType;
		if(executor == null) {
			throw new IllegalArgumentException("Null load executor");
		}
		this.executor = executor;
		if(itemInput == null) {
			throw new IllegalArgumentException("Null item input");
		}
		this.itemInput = itemInput;
		this.countLimit = countLimit > 0 ? countLimit : Long.MAX_VALUE;
		if(weight < 0) {
			throw new IllegalArgumentException("Negative load job weight");
		}
		this.weight = weight;
		this.circularFlag = isCircular;
		this.shuffleFlag = shuffleFlag;
		this.batchSize = appConfig.getItemSrcBatchSize();
		if(batchSize <= 0) {
			throw new IllegalArgumentException("Non-positive batch size");
		}
		this.itemQueueSizeLimit = appConfig.getItemQueueSizeLimit();
		if(itemQueueSizeLimit <= 0) {
			throw new IllegalArgumentException("Non-positive max item queue size");
		}
		if(rateLimit < 0) {
			throw new IllegalArgumentException("Negative rate limit");
		}
		this.rateLimitBarrier = new RateLimitBarrier<>(rateLimit);
		this.uniqueItems = new ConcurrentHashMap<>(itemQueueSizeLimit);
		//
		ioStats = new BasicIoStats(
			getName(), appConfig.getNetworkServeJmx(), appConfig.getLoadMetricsPeriod()
		);
		this.loadState = loadState;
		if(loadState != null) {
			if(loadState.isLimitReached(countLimit, appConfig.getLoadLimitTime())) {
				isLimitReached = true;
				LOG.warn(Markers.MSG, "\"{}\": nothing to do more", getName());
			}
			lastStats = loadState.getStatsSnapshot();
			this.skipCount = lastStats.getSuccCount() + lastStats.getFailCount();
			if(skipCount < 0) {
				throw new IllegalArgumentException("Negative items skip count");
			}
			this.lastItem = loadState.getLastDataItem();
			// apply parameters from loadState to current load executor
			final long
				countSucc = lastStats.getSuccCount(),
				countFail = lastStats.getFailCount();
			counterSubm.addAndGet(countSucc + countFail);
			counterResults.set(countSucc + countFail);
			ioStats.markSucc(
				countSucc, lastStats.getByteCount(), lastStats.getDurationValues(),
				lastStats.getLatencyValues()
			);
			ioStats.markFail(countFail);
			ioStats.markElapsedTime(lastStats.getElapsedTime());
			synchronized(this) {
				notifyAll();
			}
			synchronized(ioStats) {
				ioStats.notifyAll();
			}
		} else {
			skipCount = 0;
		}
		lastStats = ioStats.getSnapshot();
		//
		itemOutBuff = new LimitedQueueItemBuffer<>(
			new ArrayBlockingQueue<T>(DEFAULT_RESULTS_QUEUE_SIZE)
		);
		//
		LoadCloseHook.add(this);
	}
	//
	@Override
	public final LoadExecutor<T, A> getExecutor() {
		return executor;
	}
	//
	@Override
	public final IoStats.Snapshot getStatsSnapshot()
	throws RemoteException {
		return lastStats;
	}
	//
	@Override
	public final LoadState<T> getLoadState() {
		return new BasicLoadState.Builder<T, LoadState<T>>()
			.setAppConfig(loadState.getAppConfig())
			.setLastItem(lastItem)
			.setStatsSnapshot(lastStats)
			.build();
	}
	//
	@Override
	public final Input<T> getInput() {
		return itemInput;
	}
	//
	@Override
	public LoadType getLoadType()
	throws RemoteException {
		return loadType;
	}
	//
	@Override
	public final long getCountLimit() {
		return countLimit;
	}
	//
	@Override
	public final int getWeight() {
		return weight;
	}
	//
	@Override
	public final boolean isCircular() {
		return circularFlag;
	}
	//
	@Override
	public final boolean isShuffle() {
		return shuffleFlag;
	}
	//
	@Override
	public final void setOutput(final Output<T> itemOutput) {
		this.itemOutput = itemOutput;
	}
	//
	@Override
	public final void logMetrics(final Marker marker) {
		LOG.info(
			marker,
			Markers.PERF_SUM.equals(marker) ?
				"\"" + getName() + "\" summary: " + lastStats.toSummaryString() : lastStats
		);
	}
	//
	@Override
	public final void reset() {
		if(itemInput != null) {
			try {
				itemInput.reset();
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to reset data item input");
			}
		}
	}
	//
	private final Random rnd = new Random(); // create once instead creating every time
	private void produceLoad() {
		final List<T> buff = new ArrayList<>(batchSize);
		int n = 0, m = 0;
		try {

			n = (int) Math.min(
				itemInput.get(buff, batchSize), countLimit - counterSubm.get()
			);
			if(shuffleFlag) {
				Collections.shuffle(buff, rnd);
			}
			if(n > 0 && rateLimitBarrier.getApprovalsFor(null, n)) {
				for(m = 0; m < n && !isInterrupted(); ) {
					m += produceLoadFor(buff, m, n);
					LockSupport.parkNanos(1);
				}
				counterSubm.addAndGet(n);
			} else {
				LockSupport.parkNanos(1);
			}
			// CIRCULARITY FEATURE:
			// produce only <itemQueueSizeLimit> items in order to make it possible to enqueue
			// them infinitely
			if(circularFlag && counterSubm.get() >= itemQueueSizeLimit) {
				allItemsProducedFlag = true;
			}
		} catch(
			final EOFException | InterruptedException | ClosedByInterruptException |
				IllegalStateException ignored
		) {
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.DEBUG, e, "Failed to transfer the data items, count = {}, " +
					"batch size = {}, batch offset = {}", counterSubm, n, m
			);
		}
	}
	//
	protected int produceLoadFor(final List<T> items, final int from, final int to)
	throws IOException {
		final List<A> ioTasks = new ArrayList<>();
		for(int i = from; i < to; i ++) {
			ioTasks.add((A) new BasicIoTask<>(items.get(i), loadType));
		}
		return executor.submit(this, ioTasks, from, to);
	}
	//
	private void refreshStats() {
		lastStats = ioStats.getSnapshot();
	}
	//
	private void postProcessItems() {
		try {
			final List<T> items = new ArrayList<>(batchSize);
			final int n = itemOutBuff.get(items, batchSize);
			if(n > 0) {
				if(circularFlag) {
					int m = 0, k;
					while(m < n) {
						// process the the items again
						k = produceLoadFor(items, m, n);
						if(k > 0) {
							m += k;
						} else {
							break;
						}
						LockSupport.parkNanos(1);
					}
				} else {
					postProcessUniqueItemsFinally(items);
				}
			}
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.DEBUG, e, "Failed to feed the items to \"{}\"", itemOutput
			);
		} catch(final RejectedExecutionException e) {
			if(LOG.isTraceEnabled(Markers.ERR)) {
				LogUtil.exception(LOG, Level.TRACE, e, "\"{}\" rejected the items", itemOutput);
			}
		}
	}
	//
	protected void postProcessUniqueItemsFinally(final List<T> items) {
		// log the items if the output is not configured
		if(itemOutput == null) {
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
		} else { // put to the specific items output
			int n = items.size();
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Going to put {} items to the consumer {}",
					n, itemOutput
				);
			}
			try {
				if(!items.isEmpty()) {
					int m = 0, k;
					while(m < n) {
						k = itemOutput.put(items, m, n);
						if(k > 0) {
							m += k;
						}
						LockSupport.parkNanos(1);
					}
				}
				items.clear();
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.DEBUG, e, "Failed to feed the items to \"{}\"", itemOutput
				);
			}
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG,
					"{} items were passed to the consumer {} successfully",
					n, itemOutput
				);
			}
		}
	}
	//
	private void skipIfNecessary() {
		if(skipCount > 0) {
			try {
				itemInput.skip(skipCount);
			} catch (final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to skip {} items", skipCount);
			}
		}
	}
	//
	@Override
	public final void run() {
		//
		if(itemInput == null) {
			LOG.debug(Markers.MSG, "No item source for the producing, exiting");
			return;
		}
		skipIfNecessary();
		try {
			while(!isInterrupted()) {
				if(countLimit > counterSubm.get() && !allItemsProducedFlag) {
					produceLoad();
				}
				refreshStats();
				if(!circularFlag || allItemsProducedFlag) {
					postProcessItems();
				}
				LockSupport.parkNanos(1);
			}
		} finally {
			LOG.debug(
				Markers.MSG, "{}: produced {} items from \"{}\" for the \"{}\"",
				getName(), counterSubm, itemInput, itemOutput
			);
			try {
				itemInput.close();
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to close the item source \"{}\"", itemInput
				);
			}
		}
	}
	//
	@Override
	public final void ioTaskCompleted(final A ioTask)
	throws RemoteException {
		// producing was interrupted?
		if(isInterrupted.get()) {
			return;
		}
		final T item = ioTask.getItem();
		final IoTask.Status status = ioTask.getStatus();
		// update the metrics
		ioTask.mark(ioStats);
		if(status == IoTask.Status.SUCC) {
			lastItem = item;
			// put into the output buffer
			try {
				itemOutBuff.put(item);
				if(circularFlag) {
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
	public final int ioTaskCompletedBatch(final List<A> ioTasks, final int from, final int to)
	throws RemoteException {
		// producing was interrupted?
		if(isInterrupted.get()) {
			return 0;
		}
		//
		final int n = to - from;
		if(n > 0) {
			//
			IoTask<T> ioTask;
			T item = null;
			IoTask.Status status;
			for(int i = from; i < to; i++) {
				ioTask = ioTasks.get(i);
				item = ioTask.getItem();
				//
				status = ioTask.getStatus();
				// update the metrics
				ioTask.mark(ioStats);
				if(status == IoTask.Status.SUCC) {
					// pass data item to a consumer
					try {
						itemOutBuff.put(item);
						if(circularFlag) {
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
			if(item != null) {
				lastItem = item;
			}
			synchronized(this) {
				notifyAll();
			}
			counterResults.addAndGet(n);
		}
		//
		return n;
	}
	//
	@Override
	public final void ioTaskCancelled(final int n) {
		LOG.debug(Markers.MSG, "{}: I/O task canceled", hashCode());
		ioStats.markFail(n);
		counterResults.addAndGet(n);
	}
	//
	@Override
	public final void ioTaskFailed(final int n, final Throwable e) {
		ioStats.markFail(n);
		counterResults.addAndGet(n);
	}
	//
	@Override
	public final void start() {
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
		ioStats.start();
		//
		refreshStats();
		if(isLimitReached) {
			try {
				close();
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Couldn't close the load generator \"{}\"", getName()
				);
			}
		} else {
			if(counterResults.get() > 0) {
				skipIfNecessary();
			}
			super.start();
			LOG.debug(Markers.MSG, "Started load generator {}", getName());
		}
	}
	//
	@Override
	public final void shutdown()
	throws IllegalStateException {
		if(isStarted.get()) {
			if(isShutdown.compareAndSet(false, true)) {
				synchronized(this) {
					notifyAll();
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
		if(circularFlag) {
			allItemsProducedFlag = true; //  unblock ResultsDispatcher thread
		}
		LOG.debug(Markers.MSG, "Stopped the producing from \"{}\" for \"{}\"", itemInput, getName());
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
		long t, timeOutNanoSec = timeUnit.toNanos(timeOut);
		if(loadState != null) {
			t = TimeUnit.MICROSECONDS.toNanos(
				loadState.getStatsSnapshot().getElapsedTime()
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
			synchronized(this) {
				wait(1_000);
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
				if(!circularFlag) {
					LOG.debug(
						Markers.MSG, "{}: await exit due to \"done all submitted\" state", getName()
					);
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
			//
			LockSupport.parkNanos(1_000_000);
		}
	}
	//
	private boolean isDoneMaxCount() {
		return counterResults.get() >= countLimit;
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
	public final void interrupt() {
		if(isStarted.get()) {
			if(isInterrupted.compareAndSet(false, true)) {
				synchronized(this) {
					notifyAll();
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
					eff = lastStats.getDurationSum() / loadDurMicroSec;
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
		super.interrupt();
		//
		if(circularFlag) {
			final List<T> itemsList = Collections.list(
				Collections.enumeration(uniqueItems.values())
			);
			postProcessUniqueItemsFinally(itemsList);
		}
		uniqueItems.clear();
		//
		LOG.debug(Markers.MSG, "{} interrupted", getName());
	}
	//
	@Override
	public final void close()
	throws IOException {
		if(isClosed.compareAndSet(false, true)) {
			synchronized(this) {
				notifyAll();
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
				synchronized(this) {
					notifyAll();
				}
				interruptActually();
			}
		} finally {
			if(itemOutput != null) {
				try {
					itemOutput.close();
					LOG.debug(
						Markers.MSG, "{}: closed the consumer \"{}\" successfully",
						getName(), itemOutput
					);
				} catch(final IOException e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "{}: failed to close the consumer \"{}\"",
						getName(), itemOutput
					);
				}
			}
			LoadCloseHook.del(this);
			if(loadState != null) {
				if(RESTORED_STATES.containsKey(appConfig.getRunId())) {
					RESTORED_STATES.get(appConfig.getRunId()).remove(loadState);
				}
			}
		}
		LOG.debug(Markers.MSG, "\"{}\" closed successfully", getName());
	}
}
