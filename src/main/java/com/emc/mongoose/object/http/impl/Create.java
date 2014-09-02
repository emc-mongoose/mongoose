package com.emc.mongoose.object.http.impl;
//
import com.emc.mongoose.Consumer;
import com.emc.mongoose.Producer;
import com.emc.mongoose.conf.RunTimeConfig;
import com.emc.mongoose.data.UniformDataSource;
import com.emc.mongoose.logging.Markers;
import com.emc.mongoose.object.http.WSLoadExecutor;
import com.emc.mongoose.object.http.data.WSObject;
//
import com.emc.mongoose.object.http.api.WSRequestConfig;
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
public class Create
extends WSLoadExecutor {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public Create(
		final String[] addrs, final WSRequestConfig recConf, final long maxCount,
		final int threadsPerNode, final String listFile,
		final long minObjSize, final long maxObjSize
	) throws IOException, CloneNotSupportedException {
		super(addrs, recConf, maxCount, threadsPerNode, listFile);
		if(producer==null) { // otherwise producer is reader specified by "listFile"
			producer = new WSObjProducer(minObjSize, maxObjSize, recConf.getDataSource());
			producer.setConsumer(this);
		}
	}
	//
	final class WSObjProducer
	extends Thread
	implements Producer<WSObject> {
		//
		private final long minObjSize, maxObjSize;
		private final UniformDataSource dataSrc;
		private final ThreadLocalRandom thrLocalRnd;
		private Consumer<WSObject> consumer;
		//
		public WSObjProducer(
			final long minObjSize, final long maxObjSize, final UniformDataSource dataSrc
		) {
			this.minObjSize = minObjSize;
			this.maxObjSize = maxObjSize;
			this.dataSrc = dataSrc;
			thrLocalRnd = ThreadLocalRandom.current();
			setName(getClass().getSimpleName());
		}
		//
		@Override
		public final void setConsumer(final Consumer<WSObject> consumer)
			throws RemoteException {
			this.consumer = consumer;
		}
		//
		@Override
		public final Consumer<WSObject> getConsumer()
			throws RemoteException {
			return consumer;
		}
		//
		private void produceNextAndFeed()
		throws IOException, InterruptedException, RejectedExecutionException {
			final long nextSize = thrLocalRnd.nextLong(minObjSize, maxObjSize + 1);
			final WSObject nextObject = new WSObject(nextSize);
			consumer.submit(nextObject);
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
				for(long i=0; i<maxCount; i++) {
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
					consumer.submit(WSObject.class.cast(null));
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
