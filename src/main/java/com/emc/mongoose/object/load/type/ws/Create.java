package com.emc.mongoose.object.load.type.ws;
//
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.object.api.WSRequestConfig;
import com.emc.mongoose.object.data.WSObjectImpl;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.object.load.WSLoadExecutorBase;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
/**
 Created by kurila on 23.04.14.
 */
public class Create<T extends WSObjectImpl>
extends WSLoadExecutorBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	@SuppressWarnings("unchecked")
	public Create(
		final RunTimeConfig runTimeConfig,
		final String[] addrs, final WSRequestConfig<T> recConf, final long maxCount,
		final int threadsPerNode, final String listFile,
		final long minObjSize, final long maxObjSize
	) throws IOException, CloneNotSupportedException {
		super(runTimeConfig, addrs, recConf, maxCount, threadsPerNode, listFile);
		if(producer==null) { // otherwise producer is reader specified by "listFile"
			producer = new WSDataItemProducer(minObjSize, maxObjSize);
			producer.setConsumer(this);
		}
	}
	//
	final class WSDataItemProducer
	extends Thread
	implements Producer<T> {
		//
		private final long minObjSize, maxObjSize;
		private Consumer<T> consumer;
		//
		public WSDataItemProducer(final long minObjSize, final long maxObjSize) {
			this.minObjSize = minObjSize;
			this.maxObjSize = maxObjSize;
			setName(getClass().getSimpleName());
		}
		//
		@Override
		public final void setConsumer(final Consumer<T> consumer) {
			this.consumer = consumer;
		}
		//
		@Override
		public final Consumer<T> getConsumer()
		throws RemoteException {
			return consumer;
		}
		//
		@SuppressWarnings("unchecked")
		private void produceNextAndFeed()
		throws IOException, InterruptedException, RejectedExecutionException {
			consumer.submit(
				(T) new WSObjectImpl(
					ThreadLocalRandom.current().nextLong(minObjSize, maxObjSize + 1)
				)
			);
		}
		//
		@Override
		public final void run() {
			try {
				LOG.debug(
					Markers.MSG, "Will try to produce up to {} objects of {} size", maxCount,
					minObjSize==maxObjSize ?
						RunTimeConfig.formatSize(minObjSize)
							:
						RunTimeConfig.formatSize(minObjSize)+".."+RunTimeConfig.formatSize(maxObjSize)
				);
				for(long i = 0; i < maxCount; i ++) {
					try {
						produceNextAndFeed();
						LOG.trace(Markers.MSG, "Submitted object #{}", i);
					} catch(final RejectedExecutionException e) {
						LOG.trace(Markers.ERR, "Submitting the object rejected by consumer");
					} catch(final IOException e) {
						LOG.trace(Markers.ERR, "Failed to submit object to consumer", e);
					}
				}
				try {
					consumer.submit(null);
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
