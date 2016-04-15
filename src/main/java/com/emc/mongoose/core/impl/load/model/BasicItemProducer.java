package com.emc.mongoose.core.impl.load.model;
//
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.load.barrier.Barrier;
import com.emc.mongoose.core.api.load.model.ItemProducer;
//
import com.emc.mongoose.core.impl.load.barrier.RateLimitBarrier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.LockSupport;

/**
 Created by kurila on 19.06.15.
 */
public class BasicItemProducer<T extends Item>
extends Thread
implements ItemProducer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final ConcurrentMap<String, T> uniqueItems;
	protected final Input<T> itemInput;
	protected final long maxCount;
	protected final boolean isCircular;
	protected final boolean isShuffling;
	protected final Barrier<T> rateLimitBarrier;
	protected final int batchSize;
	protected volatile Output<T> itemOutput = null;
	protected long skipCount;
	protected T lastItem;
	protected int maxItemQueueSize;
	//
	protected volatile boolean allItemsProducedFlag = false;
	protected volatile long producedItemsCount = 0;
	//
	protected BasicItemProducer(
		final Input<T> itemInput, final long maxCount, final int batchSize,
		final boolean isCircular, final boolean isShuffling, final int maxItemQueueSize,
	    final float rateLimit
	) {
		this(itemInput, maxCount, batchSize, isCircular, isShuffling, maxItemQueueSize, rateLimit,
			0, null
		);
	}
	//
	private BasicItemProducer(
		final Input<T> itemInput, final long maxCount, final int batchSize,
		final boolean isCircular, final boolean isShuffling, final int maxItemQueueSize,
		final float rateLimit,
		final long skipCount, final T lastItem
	) {
		this.itemInput = itemInput;
		this.maxCount = maxCount - skipCount;
		this.batchSize = batchSize;
		this.skipCount = skipCount;
		this.lastItem = lastItem;
		this.isCircular = isCircular;
		this.isShuffling = isShuffling;
		this.maxItemQueueSize = maxItemQueueSize;
		this.rateLimitBarrier = new RateLimitBarrier<>(rateLimit);
		this.uniqueItems = new ConcurrentHashMap<>(maxItemQueueSize);
		setDaemon(true);
	}
	//
	@Override
	public void setSkipCount(final long itemsCount) {
		this.skipCount = itemsCount;
	}
	//
	@Override
	public void setLastItem(final T dataItem) {
		this.lastItem = dataItem;
	}
	//
	@Override
	public void setOutput(final Output<T> itemOutput)
	throws RemoteException {
		this.itemOutput = itemOutput;
	}
	//
	public Input<T> getInput()
	throws RemoteException {
		return itemInput;
	}
	//
	@Override
	public void reset() {
		if(itemInput != null) {
			try {
				itemInput.reset();
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to reset data item input");
			}
		}
	}
	//
	@Override
	public final void run() {
		runActually();
	}
	//
	private final Random rnd = new Random(); // create once instead creating every time
	//
	protected void runActually() {
		//
		if(itemInput == null) {
			LOG.debug(Markers.MSG, "No item source for the producing, exiting");
			return;
		}
		int n = 0, m = 0;
		try {
			List<T> buff;
			while(maxCount > producedItemsCount && !isInterrupted) {
				try {
					buff = new ArrayList<>(batchSize);
					n = (int) Math.min(itemInput.get(buff, batchSize), maxCount - producedItemsCount);
					if(isShuffling) {
						Collections.shuffle(buff, rnd);
					}
					if(isInterrupted) {
						break;
					}
					if(n > 0 && rateLimitBarrier.getApprovalsFor(null, n)) {
						for(m = 0; m < n && !isInterrupted; ) {
							m += itemOutput.put(buff, m, n);
							LockSupport.parkNanos(1);
						}
						producedItemsCount += n;
					} else {
						if(isInterrupted) {
							break;
						}
					}
					// CIRCULARITY FEATURE:
					// produce only <maxItemQueueSize> items in order to make it possible to enqueue
					// them infinitely
					if(isCircular && producedItemsCount >= maxItemQueueSize) {
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
						"batch size = {}, batch offset = {}", producedItemsCount, n, m
					);
				}
			}
		} finally {
			LOG.debug(
				Markers.MSG, "{}: produced {} items from \"{}\" for the \"{}\"",
				getName(), producedItemsCount, itemInput, itemOutput
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
	private volatile boolean isInterrupted = false;
	//
	@Override
	public void interrupt()
	throws IllegalStateException {
		isInterrupted = true;
	}
}
