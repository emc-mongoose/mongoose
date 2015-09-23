package com.emc.mongoose.core.impl.load.model;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemDst;
import com.emc.mongoose.core.api.data.model.DataItemSrc;
import com.emc.mongoose.core.api.load.model.Producer;
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
public class DataItemProducer<T extends DataItem>
extends Thread
implements Producer<T> {
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
	public DataItemProducer(final DataItemSrc<T> itemSrc, final int batchSize) {
		this(itemSrc, batchSize, false);
	}
	//
	public DataItemProducer(
		final DataItemSrc<T> itemSrc, final int batchSize, final boolean isCircular
	) {
		this(itemSrc, batchSize, isCircular, 0, null);
	}
	//
	public DataItemProducer(
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
		int n, m;
		try {
			do {
				try {
					n = itemSrc.get(buff, batchSize);
					if(n > 0) {
						for(m = 0; m < n; m += itemDst.put(buff, m, n)) {
							LockSupport.parkNanos(1);
						}
						count += n;
					} else {
						LockSupport.parkNanos(1);
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
					LogUtil.exception(LOG, Level.WARN, e, "Failed to transfer the data items");
				}
			} while(!isInterrupted());
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "{}: producing is interrupted", itemSrc);
		} finally {
			LOG.debug(
				Markers.MSG, "{}: produced {} items, shutting down the destination \"{}\"", itemSrc,
				count, itemDst
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
				LogUtil.exception(LOG, Level.WARN, e,
					"Failed to skip such amount of data items - \"{}\"", skipCount);
			}
		}
	}
}
