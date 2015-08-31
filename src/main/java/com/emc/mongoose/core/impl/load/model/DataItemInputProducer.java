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
import java.rmi.RemoteException;
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
	protected DataItemInput<T> itemIn;
	protected volatile Consumer<T> consumer = null;
	protected long inStreamOffset;
	protected String lastItemId;
	//
	public DataItemInputProducer(final DataItemInput<T> itemIn) {
		this(itemIn, 0);
	}
	//
	public DataItemInputProducer(final DataItemInput<T> itemIn, final long inStreamOffset) {
		this.itemIn = itemIn;
		this.inStreamOffset = inStreamOffset;
		setDaemon(true);
		setName("dataItemInputProducer<" + itemIn.toString() + ">");
	}
	//
	public void setInStreamOffset(final long inStreamOffset) {
		this.inStreamOffset = inStreamOffset;
	}
	//
	public long getInStreamOffset() {
		return inStreamOffset;
	}
	//
	public void setLastItemId(final String lastItemId) {
		this.lastItemId = lastItemId;
	}
	//
	public String getLastItemId() {
		return lastItemId;
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
	@Override
	public final void run() {
		T nextItem = null;
		long count = 0;
		if(consumer == null) {
			LOG.warn(Markers.ERR, "Have no consumer set, exiting");
			return;
		}
		if (inStreamOffset > 0) {
			try {
				itemIn.setLastItemId(lastItemId);
				itemIn.skip(inStreamOffset);
			} catch (final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e,
					"Failed to skip such amount of data items - \"{}\"", inStreamOffset);
			}
		}
		try {
			do {
				try {
					nextItem = itemIn.read();
				} catch(final EOFException e) {
					break;
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to read the next data item");
				}
				if(nextItem == null) {
					break;
				} else {
					try {
						consumer.submit(nextItem);
						count ++;
					} catch(final RemoteException e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Failed to submit remotely the next data item"
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
			} while(true);
		} catch(final InterruptedException ignore) {
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
}
