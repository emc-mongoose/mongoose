package com.emc.mongoose.core.impl.load.model;
//
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.load.model.Producer;
//
import com.emc.mongoose.common.logging.LogUtil;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
/**
 Created by kurila on 15.12.14.
 */
public abstract class DataItemGeneratorBase<T extends DataItem>
extends Thread
implements Producer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final long maxCount, minObjSize, maxObjSize;
	protected final float objSizeBias;
	protected Consumer<T> newDataConsumer;
	//
	protected DataItemGeneratorBase(
		final long maxCount, final long minObjSize, final long maxObjSize, final float objSizeBias
	) {
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		this.minObjSize = minObjSize;
		this.maxObjSize = maxObjSize;
		this.objSizeBias = objSizeBias;
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
	protected abstract T produceSpecificDataItem(final long nextSize)
	throws IOException;
	//
	private final static String
		MSG_SUBMIT_REJECTED = "Submitting the object rejected by consumer",
		MSG_SUBMIT_FAILED = "Failed to submit object to consumer",
		MSG_INTERRUPTED = "Interrupted";
	//
	@Override
	public final void run() {
		final long sizeRange = maxObjSize - minObjSize;
		final ThreadLocalRandom thrLocalRnd = ThreadLocalRandom.current();
		long i = 0, nextSize;
		//
		LOG.debug(
			LogUtil.MSG, "Will try to produce up to {} objects of {} size", maxCount,
			minObjSize == maxObjSize ?
				SizeUtil.formatSize(minObjSize) :
				SizeUtil.formatSize(minObjSize)+".."+ SizeUtil.formatSize(maxObjSize)
		);
		//
		do {
			try {
				if(minObjSize == maxObjSize) {
					nextSize = minObjSize;
				} else {
					if(objSizeBias == 1) {
						nextSize = (long) (thrLocalRnd.nextDouble() * sizeRange);
					} else {
						nextSize = (long) (Math.pow(thrLocalRnd.nextDouble(), objSizeBias) * sizeRange);
					}
					nextSize += minObjSize;
				}
				newDataConsumer.submit(produceSpecificDataItem(nextSize));
				if(LOG.isTraceEnabled(LogUtil.MSG)) {
					LOG.trace(
						LogUtil.MSG, "Submitted object #{} of size {}",
						i, SizeUtil.formatSize(nextSize)
					);
				}
				i ++;
			} catch(final RejectedExecutionException e) {
				LOG.trace(LogUtil.ERR, MSG_SUBMIT_REJECTED);
			} catch(final IOException e) {
				LogUtil.failure(LOG, Level.TRACE, e, MSG_SUBMIT_FAILED);
			} catch(final InterruptedException e) {
				LogUtil.trace(LOG, Level.DEBUG, LogUtil.MSG, MSG_INTERRUPTED);
				break;
			}
		} while(!isInterrupted() && i < maxCount);
		LOG.debug(LogUtil.MSG, "Finished, generated {} items", i);
	}
	//
}
