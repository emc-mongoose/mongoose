package com.emc.mongoose.util.client.impl;
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
	//
	public DataItemInputProducer(final DataItemInput<T> itemIn) {
		this.itemIn = itemIn;
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
		if(consumer == null) {
			LOG.warn(Markers.ERR, "Have no consumer set, exiting");
			return;
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
					} catch(final RemoteException e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Failed to submit remotely the next data item"
						);
					} catch(final RejectedExecutionException e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Consumer \"{}\" rejected the data item", consumer
						);
					}
				}
			} while(true);
		} catch(final InterruptedException ignore) {
		} finally {
			LOG.debug(
				Markers.MSG, "{}: producing done, shutting down the consumer \"{}\"",
				itemIn, consumer
			);
			try {
				consumer.shutdown();
			} catch(final RemoteException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to shut down remotely the consumer");
			}
		}
	}
}
