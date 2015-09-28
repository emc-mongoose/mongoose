package com.emc.mongoose.core.impl.load.model;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemDst;
import com.emc.mongoose.core.api.data.model.DataItemSrc;
import com.emc.mongoose.core.api.load.model.DataItemProducer;
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
import java.util.List;
import java.util.concurrent.locks.LockSupport;
/**
 Created by kurila on 19.06.15.
 */
public class BasicDataItemProducer<T extends DataItem>
extends Thread
implements DataItemProducer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final DataItemSrc<T> itemSrc;
	protected volatile DataItemDst<T> itemDst = null;
	protected final int batchSize;
	protected final List<T> buff;
	protected long skipCount;
	protected T lastDataItem;
	protected boolean isCircular;
	//
	public BasicDataItemProducer(final DataItemSrc<T> itemSrc, final int batchSize) {
		this(itemSrc, batchSize, false);
	}
	//
	public BasicDataItemProducer(
		final DataItemSrc<T> itemSrc, final int batchSize, final boolean isCircular
	) {
		this(itemSrc, batchSize, isCircular, 0, null);
	}
	//
	public BasicDataItemProducer(
		final DataItemSrc<T> itemSrc, final int batchSize, final boolean isCircular,
		final long skipCount, final T lastDataItem
	) {
		this.itemSrc = itemSrc;
		this.batchSize = batchSize;
		this.buff = new ArrayList<>(batchSize);
		this.skipCount = skipCount;
		this.lastDataItem = lastDataItem;
		this.isCircular = isCircular;
	}
	//
	@Override
	public void setSkipCount(final long itemsCount) {
		this.skipCount = itemsCount;
	}
	//
	@Override
	public void setLastDataItem(final T dataItem) {
		this.lastDataItem = dataItem;
	}
	//
	@Override
	public void setDataItemDst(final DataItemDst<T> itemDst)
	throws RemoteException {
		this.itemDst = itemDst;
	}
	//
	@Override
	public DataItemSrc<T> getDataItemSrc()
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
		if(itemDst == null) {
			LOG.warn(Markers.ERR, "Have no item destination set, exiting");
			return;
		}
		if(itemSrc == null) {
			LOG.debug(Markers.MSG, "No item source for the producing, exiting");
			return;
		}
		//
		skipIfNecessary();
		//
		long count = 0;
		int n = 0, m = 0;
		try {
			do {
				try {
					n = itemSrc.get(buff, batchSize);
					if(isInterrupted) {
						break;
					}
					if(n > 0) {
						for(m = 0; m < n && !isInterrupted;) {
							m += itemDst.put(buff, m, n);
							LockSupport.parkNanos(1);
						}
						buff.clear();
						if(isInterrupted) {
							break;
						}
						count += n;
					} else {
						if(isInterrupted) {
							break;
						} else {
							LockSupport.parkNanos(1);
						}
					}
				} catch(final EOFException e) {
					if(isCircular) {
						reset();
					} else {
						break;
					}
				} catch(final ClosedByInterruptException | IllegalStateException e) {
					break;
				} catch(final IOException e) {
					LogUtil.exception(
						LOG, Level.WARN, e,
						"Failed to transfer the data items, " +
						"count = {}, batch size = {}, batch offset = {}", count, n, m
					);
				}
			} while(!isInterrupted);
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "{}: producing is interrupted", itemSrc);
		} finally {
			LOG.debug(
				Markers.MSG, "{}: produced {} items from \"{}\" for the \"{}\"",
				getName(), count, itemSrc, itemDst
			);
		}
	}
	//
	private void skipIfNecessary() {
		if(skipCount > 0) {
			try {
				itemSrc.setLastDataItem(lastDataItem);
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
}
