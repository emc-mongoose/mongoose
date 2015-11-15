package com.emc.mongoose.core.impl.load.model;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.Item;
import com.emc.mongoose.core.api.data.model.ItemDst;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.load.model.ItemProducer;
//
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
	protected final ItemSrc<T> itemSrc;
	protected final long maxCount;
	protected volatile ItemDst<T> itemDst = null;
	protected final int batchSize;
	protected long skipCount;
	protected T lastDataItem;
	protected boolean isCircular;
	protected boolean isShuffling;
	//
	protected long circularSleepTimeMillis = 0;
	//
	protected BasicItemProducer(
		final ItemSrc<T> itemSrc, final long maxCount, final int batchSize,
		final boolean isCircular, final boolean isShuffling
	) {
		this(itemSrc, maxCount, batchSize, isCircular, isShuffling, 0, null);
	}
	//
	private BasicItemProducer(
		final ItemSrc<T> itemSrc, final long maxCount, final int batchSize,
		final boolean isCircular, final boolean isShuffling,
		final long skipCount, final T lastDataItem
	) {
		this.itemSrc = itemSrc;
		this.maxCount = maxCount - skipCount;
		this.batchSize = batchSize;
		this.skipCount = skipCount;
		this.lastDataItem = lastDataItem;
		this.isCircular = isCircular;
		this.isShuffling = isShuffling;
	}
	//
	@Override
	public void setSkipCount(final long itemsCount) {
		this.skipCount = itemsCount;
	}
	//
	@Override
	public void setLastItem(final T dataItem) {
		this.lastDataItem = dataItem;
	}
	//
	@Override
	public void setItemDst(final ItemDst<T> itemDst)
	throws RemoteException {
		this.itemDst = itemDst;
	}
	//
	@Override
	public ItemSrc<T> getItemSrc()
	throws RemoteException {
		return itemSrc;
	}
	//
	@Override
	public void reset() {
		if(itemSrc != null) {
			try {
				itemSrc.reset();
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
	protected void runActually() {
		//
		if(itemSrc == null) {
			LOG.debug(Markers.MSG, "No item source for the producing, exiting");
			return;
		}
		long count = 0;
		int n = 0, m = 0;
		try {
			List<T> buff;
			while(!isInterrupted && count < maxCount) {
				try {
					buff = new ArrayList<>(batchSize);
					n = itemSrc.get(buff, batchSize);
					if(isShuffling) {
						Collections.shuffle(buff);
					}
					if(isInterrupted) {
						break;
					}
					if(n > 0) {
						for(m = 0; m < n && !isInterrupted;) {
							m += itemDst.put(buff, m, n);
							LockSupport.parkNanos(1);
						}
						count += n;
					} else {
						if(isInterrupted) {
							break;
						}
					}
				} catch(final EOFException e) {
					if(isCircular) {
						try {
							circularSleepTimeMillis
								= (circularSleepTimeMillis == 0)
									? (count / 2) : circularSleepTimeMillis;
							// prevent a lot of calls to put method of load server[s]
							Thread.sleep(circularSleepTimeMillis);
						} catch(final InterruptedException ex) {
							LogUtil.exception(LOG, Level.WARN, ex, "Interrupted");
						} finally {
							reset();
						}
					} else {
						break;
					}
				} catch(final ClosedByInterruptException | IllegalStateException e) {
					break;
				} catch(final IOException e) {
					LogUtil.exception(
						LOG, Level.DEBUG, e,
						"Failed to transfer the data items, " +
						"count = {}, batch size = {}, batch offset = {}", count, n, m
					);
				}
			}
		} finally {
			LOG.debug(
				Markers.MSG, "{}: produced {} items from \"{}\" for the \"{}\"",
				getName(), count, itemSrc, itemDst
			);
			try {
				itemSrc.close();
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to close the item source \"{}\"", itemSrc
				);
			}
		}
	}
	//
	protected void skipIfNecessary() {
		if(skipCount > 0) {
			try {
				itemSrc.setLastItem(lastDataItem);
				itemSrc.skip(skipCount);
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
	//
	public void interruptProducer() {
		super.interrupt();
	}
}
