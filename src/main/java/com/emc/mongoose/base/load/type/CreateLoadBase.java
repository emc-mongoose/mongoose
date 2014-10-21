package com.emc.mongoose.base.load.type;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.load.impl.LoadExecutorBase;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.RejectedExecutionException;
/**
 Created by kurila on 20.10.14.
 Custom implementation should at least define methods: "newDataItemsProducer" returning new data items
 producer instance and "produceNextAndFeed" creating one data item and passing it to consumer.
 */
public abstract class CreateLoadBase<T extends DataItem>
extends LoadExecutorBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected CreateLoadBase(
		final RunTimeConfig runTimeConfig,
		final String[] addrs, final RequestConfig<T> recConf, final long maxCount,
		final int threadsPerNode, final String listFile,
		final long minObjSize, final long maxObjSize
	) throws IOException, CloneNotSupportedException {
		super(runTimeConfig, addrs, recConf, maxCount, threadsPerNode, listFile);
		if(producer==null) { // otherwise producer is reader specified by "listFile"
			producer = newDataItemProducer(minObjSize, maxObjSize);
			producer.setConsumer(this);
		}
	}
	//
	protected abstract Producer<T> newDataItemProducer(final long minObjSize, final long maxObjSize);
	//
	protected abstract class DataItemProducerBase
	extends Thread
	implements Producer<T> {
		//
		protected final long minObjSize, maxObjSize;
		protected Consumer<T> newDataConsumer;
		//
		protected DataItemProducerBase(final long minObjSize, final long maxObjSize) {
			this.minObjSize = minObjSize;
			this.maxObjSize = maxObjSize;
			setName(getClass().getSimpleName());
		}
		//
		@Override
		public final void setConsumer(final Consumer<T> consumer) {
			this.newDataConsumer = consumer;
		}
		//
		@Override
		public final Consumer<T> getConsumer()
			throws RemoteException {
			return newDataConsumer;
		}
		//
		protected abstract void produceNextAndFeed()
		throws IOException, InterruptedException, RejectedExecutionException;
		//
		@Override
		public final void run() {
			try {
				//
				LOG.debug(
					Markers.MSG, "Will try to produce up to {} objects of {} size", maxCount,
					minObjSize==maxObjSize ?
						RunTimeConfig.formatSize(minObjSize)
						:
						RunTimeConfig.formatSize(minObjSize)+".."+RunTimeConfig.formatSize(maxObjSize)
				);
				//
				long i = 0;
				do {
					try {
						produceNextAndFeed();
						LOG.trace(Markers.MSG, "Submitted object #{}", i);
						i ++;
					} catch(final RejectedExecutionException e) {
						LOG.trace(Markers.ERR, "Submitting the object rejected by consumer");
					} catch(final IOException e) {
						LOG.trace(Markers.ERR, "Failed to submit object to consumer", e);
					}
				} while(!isInterrupted());
				try {
					newDataConsumer.submit(null);
				} catch(final RejectedExecutionException e) {
					LOG.debug(Markers.ERR, "Consumer rejected the poison");
				}
				LOG.debug(Markers.MSG, "Generated {} items", maxCount);
			} catch(final IOException e) {
				LOG.debug(Markers.ERR, "Failed to submit object to consumer", e);
			} catch(final InterruptedException e) {
				LOG.debug(Markers.ERR, "Interrupted while submitting the object to consumer");
			} finally {
				LOG.debug(Markers.MSG, "Object producer finished");
			}
		}
		//
	}
	//
}
