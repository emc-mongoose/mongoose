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
import com.emc.mongoose.core.api.load.barrier.Barrier;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.generator.LoadGenerator;
import com.emc.mongoose.core.api.load.generator.LoadState;
import com.emc.mongoose.core.api.load.metrics.IoStats;
//
import com.emc.mongoose.core.impl.load.barrier.RateLimitBarrier;
//
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 Created by kurila on 19.06.15.
 */
public class BasicLoadGenerator<T extends Item, A extends IoTask<T>>
implements LoadGenerator<T, A> {
	//
	private final static Logger LOG = LogManager.getLogger();
	// immutable properties
	private final String name;
	private final LoadType loadType;
	private final LoadExecutor<T> executor;
	private final Input<T> itemInput;
	private final long countLimit;
	private final int weight;
	private final boolean circularFlag;
	private final boolean shuffleFlag;
	private final LoadState<T> state;
	// mutable properties
	private volatile Output<T> itemOutput = null;
	// other properties
	private final long skipCount;
	private final T lastItem;
	private final Map<String, T> uniqueItems;
	private final Barrier<T> rateLimitBarrier;
	private final int batchSize;
	private final int itemQueueSizeLimit;
	// internal states
	private IoStats ioStats;
	private final AtomicReference<IoStats.Snapshot> lastStatsRef = new AtomicReference<>();
	private final AtomicLong counterProduced = new AtomicLong(0);
	private final AtomicLong counterSubm = new AtomicLong(0);
	private final AtomicLong counterResults = new AtomicLong(0);
	private final AtomicBoolean allItemsProducedFlag = new AtomicBoolean(false);
	private final AtomicBoolean isStarted = new AtomicBoolean(false);
	private final AtomicBoolean isShutdown = new AtomicBoolean(false);
	private final AtomicBoolean isInterrupted = new AtomicBoolean(false);
	private final AtomicBoolean isClosed = new AtomicBoolean(false);
	//
	public BasicLoadGenerator(
		final AppConfig appConfig, final String name, final LoadType loadType,
		final LoadExecutor<T> executor, final Input<T> itemInput, final long countLimit,
		final int weight, final float rateLimit, final boolean isCircular, final boolean shuffleFlag
	) {
		this(
			name, loadType, executor, itemInput, countLimit, weight, rateLimit, isCircular, shuffleFlag,
			new BasicLoadState.Builder<T, LoadState<T>>()
				.setAppConfig(appConfig)
				.setLastItem(null)
				.setLoadNumber(0)
				.setStatsSnapshot(null)
				.build(),
			0
		);
	}
	//
	public BasicLoadGenerator(
		final String name, final LoadType loadType, final LoadExecutor<T> executor,
		final Input<T> itemInput, final long countLimit, final int weight, final float rateLimit,
		final boolean isCircular, final boolean shuffleFlag, final LoadState<T> state,
		final long skipCount
	) throws IllegalStateException {
		if(name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Empty load job name");
		}
		this.name = name;
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
		if(countLimit < 0) {
			throw new IllegalArgumentException("Negative count limit");
		}
		this.countLimit = countLimit;
		if(weight < 0) {
			throw new IllegalArgumentException("Negative load job weight");
		}
		this.weight = weight;
		this.circularFlag = isCircular;
		this.shuffleFlag = shuffleFlag;
		if(state == null) {
			throw new IllegalArgumentException("State is null");
		}
		this.state = state;
		final AppConfig config = state.getAppConfig();
		this.batchSize = config.getItemSrcBatchSize();
		if(batchSize <= 0) {
			throw new IllegalArgumentException("Non-positive batch size");
		}
		if(skipCount < 0) {
			throw new IllegalArgumentException("Negative items skip count");
		}
		this.skipCount = skipCount;
		this.lastItem = state.getLastDataItem();
		this.itemQueueSizeLimit = config.getItemQueueSizeLimit();
		if(itemQueueSizeLimit <= 0) {
			throw new IllegalArgumentException("Non-positive max item queue size");
		}
		if(rateLimit < 0) {
			throw new IllegalArgumentException("Negative rate limit");
		}
		this.rateLimitBarrier = new RateLimitBarrier<>(rateLimit);
		this.uniqueItems = new ConcurrentHashMap<>(itemQueueSizeLimit);
	}
	//
	@Override
	public final String getName() {
		return name;
	}
	//
	@Override
	public final LoadExecutor<T> getExecutor() {
		return executor;
	}
	//
	@Override
	public final IoStats.Snapshot getStatsSnapshot()
	throws RemoteException {
		return lastStatsRef.get();
	}
	//
	@Override
	public final LoadState<T> getLoadState() {
		return new BasicLoadState.Builder<T, LoadState<T>>()
			.setAppConfig(state.getAppConfig())
			.setLastItem(lastItem)
			.setLoadNumber(0)
			.setStatsSnapshot(lastStatsRef.get())
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
	public final void setOutput(final Output<T> itemOutput)
	throws RemoteException {
		this.itemOutput = itemOutput;
	}
	//
	@Override
	public final void ioTaskCompleted(final A ioTask)
	throws RemoteException {
	}
	//
	@Override
	public final int ioTaskCompletedBatch(final List<A> ioTasks, final int from, final int to)
	throws RemoteException {
		return 0;
	}
	//
	@Override
	public final void logMetrics(final Marker marker)
	throws RemoteException {
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
	//
	@Override
	public final void run() {
		//
		if(itemInput == null) {
			LOG.debug(Markers.MSG, "No item source for the producing, exiting");
			return;
		}
		skipIfNecessary();
		int n = 0, m = 0;
		try {
			List<T> buff;
			while(countLimit > counterProduced.get() && !isInterrupted()) {
				try {
					buff = new ArrayList<>(batchSize);
					n = (int) Math.min(
						itemInput.get(buff, batchSize), countLimit - counterProduced.get()
					);
					if(shuffleFlag) {
						Collections.shuffle(buff, rnd);
					}
					if(isInterrupted()) {
						break;
					}
					if(n > 0 && rateLimitBarrier.getApprovalsFor(null, n)) {
						for(m = 0; m < n && !isInterrupted(); ) {
							m += executor.submit(buff, loadType);
							LockSupport.parkNanos(1);
						}
						counterProduced.addAndGet(n);
						LockSupport.parkNanos(1);
					} else {
						if(isInterrupted()) {
							break;
						}
					}
					// CIRCULARITY FEATURE:
					// produce only <itemQueueSizeLimit> items in order to make it possible to enqueue
					// them infinitely
					if(circularFlag && counterProduced.get() >= itemQueueSizeLimit) {
						break;
					}
				} catch(
					final EOFException | InterruptedException | ClosedByInterruptException |
					IllegalStateException e
				) {
					break;
				} catch(final IOException e) {
					LogUtil.exception(
						LOG, Level.DEBUG, e, "Failed to transfer the data items, count = {}, " +
						"batch size = {}, batch offset = {}", counterProduced, n, m
					);
				}
			}
		} finally {
			LOG.debug(
				Markers.MSG, "{}: produced {} items from \"{}\" for the \"{}\"",
				getName(), counterProduced, itemInput, itemOutput
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
	protected void skipIfNecessary() {
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
	public void shutdown()
	throws RemoteException, IllegalStateException {
		if(isShutdown.compareAndSet(false, true)) {
			// TODO
		}
	}
	//
	@Override
	public void await()
	throws RemoteException, InterruptedException {
		await(Long.MAX_VALUE, TimeUnit.DAYS);
	}
	//
	@Override
	public void await(final long timeOut, final TimeUnit timeUnit)
	throws RemoteException, InterruptedException {
	}
	//
	@Override
	public void close()
	throws IOException {
	}
}
