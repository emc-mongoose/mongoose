package com.emc.mongoose.core.impl.load.model;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemInput;
import com.emc.mongoose.core.api.load.model.Consumer;
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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 19.06.15.
 */
public class DataItemInputProducer<T extends DataItem>
extends Thread
implements Producer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final DataItemInput<T> itemIn;
	protected final int batchSize;
	protected final List<T> buff;
	protected volatile Consumer<T> consumer = null;
	protected long skippedItemsCount;
	protected T lastDataItem;
	protected boolean isCircular;
	//
	public DataItemInputProducer(final DataItemInput<T> itemIn, final int batchSize) {
		this(itemIn, batchSize, false);
	}
	//
	public DataItemInputProducer(
		final DataItemInput<T> itemIn, final int batchSize, final boolean isCircular
	) {
		this(itemIn, batchSize, isCircular, 0, null);
	}
	//
	public DataItemInputProducer(
		final DataItemInput<T> itemIn, final int batchSize, final boolean isCircular,
		final long skippedItemsCount, final T dataItem
	) {
		this.itemIn = itemIn;
		this.batchSize = batchSize;
		this.buff = new ArrayList<>(batchSize);
		this.skippedItemsCount = skippedItemsCount;
		this.lastDataItem = dataItem;
		this.isCircular = isCircular;
		setDaemon(true);
		setName("dataItemInputProducer<" + itemIn.toString() + ">");
	}
	//
	public void setSkippedItemsCount(final long itemsCount) {
		this.skippedItemsCount = itemsCount;
	}
	//
	public long getSkippedItemsCount() {
		return skippedItemsCount;
	}
	//
	public void setLastDataItem(final T dataItem) {
		this.lastDataItem = dataItem;
	}
	//
	public T getLastDataItem() {
		return lastDataItem;
	}
	//
	@Override
	public void setConsumer(final Consumer<T> consumer)
	throws RemoteException {
		this.consumer = consumer;
	}
	//
	@Override
	public Consumer<T> getConsumer()
	throws RemoteException {
		return consumer;
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
		timeUnit.timedJoin(this, timeOut);
	}
	//
	public void reset() {
		try {
			itemIn.reset();
		} catch (final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to reset data item input");
		}
	}
	//
	@Override
	public final void run() {
		long count = 0;
		int n = 0;
		if(consumer == null) {
			LOG.warn(Markers.ERR, "Have no consumer set, exiting");
			return;
		}
		if (skippedItemsCount > 0) {
			try {
				itemIn.setLastDataItem(lastDataItem);
				itemIn.skip(skippedItemsCount);
			} catch (final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e,
					"Failed to skip such amount of data items - \"{}\"", skippedItemsCount);
			}
		}
		try {
			do {
				try {
					n = itemIn.read(buff, batchSize);
				} catch (final EOFException e) {
					if (isCircular) {
						reset();
						continue;
					} else {
						break;
					}
				} catch(final ClosedByInterruptException | IllegalStateException e) {
					break;
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to read the next data item");
				}
				if(n == 0) {
					if (isCircular) {
						reset();
					} else {
						break;
					}
				} else {
					try {
						consumer.feedBatch(buff.subList(0, n));
						count += n;
					} catch(final RemoteException e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Failed to feed remotely the next data item"
						);
					} catch(final RejectedExecutionException e) {
						if(LOG.isTraceEnabled(Markers.ERR)) {
							LogUtil.exception(
								LOG, Level.TRACE, e, "Consumer \"{}\" rejected the data item",
								consumer
							);
						}
					}
				}
			} while(!isInterrupted());
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "{}: producing is interrupted", itemIn);
		} finally {
			LOG.debug(
				Markers.MSG, "{}: produced {} items, shutting down the consumer \"{}\"",
				itemIn, count, consumer
			);
			try {
				consumer.shutdown();
			} catch(final RemoteException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to shut down remotely the consumer");
			}
		}
	}
	//
	@Override
	public final void interrupt() {
		super.interrupt();
	}
}
