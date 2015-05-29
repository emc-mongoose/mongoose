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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 15.12.14.
 */
public final class BasicDataItemGenerator<T extends DataItem>
extends Thread
implements Producer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final long maxCount, minObjSize, maxObjSize;
	private final float objSizeBias;
	private final Constructor<T> dataConstructor;
	private Consumer<T> newDataConsumer;
	//
	public BasicDataItemGenerator(
		final Class<T> dataCls,
		final long maxCount, final long minObjSize, final long maxObjSize, final float objSizeBias
	) throws NoSuchMethodException {
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		this.minObjSize = minObjSize;
		this.maxObjSize = maxObjSize;
		this.objSizeBias = objSizeBias;
		dataConstructor = dataCls.getConstructor(Long.class);
		setName("dataItemGenerator<" + dataCls.getSimpleName() + ">" + hashCode());
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
	@Override
	public final void await()
	throws RemoteException, InterruptedException {
		join();
	}
	//
	@Override
	public final void await(final long timeOut, final TimeUnit timeUnit)
	throws RemoteException, InterruptedException {
		timeUnit.timedJoin(this, timeOut);
	}
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
		//
		LOG.debug(
			LogUtil.MSG, "Will try to produce up to {} objects of {} size", maxCount,
			minObjSize == maxObjSize ?
				SizeUtil.formatSize(minObjSize) :
				SizeUtil.formatSize(minObjSize)+".."+ SizeUtil.formatSize(maxObjSize)
		);
		//
		long i = 0, nextSize;
		while(i < maxCount && !isInterrupted()) {
			try {
				if(minObjSize == maxObjSize) {
					nextSize = minObjSize;
				} else {
					if(objSizeBias == 1) {
						nextSize = (long) (thrLocalRnd.nextDouble() * sizeRange);
					} else {
						nextSize = (long) (
							Math.pow(thrLocalRnd.nextDouble(), objSizeBias) * sizeRange
						);
					}
					nextSize += minObjSize;
				}
				newDataConsumer.submit(dataConstructor.newInstance(nextSize));
				i ++;
				if(LOG.isTraceEnabled(LogUtil.MSG)) {
					LOG.trace(
						LogUtil.MSG, "Submitted object #{} of size {}",
						i, SizeUtil.formatSize(nextSize)
					);
				}
			} catch(final RejectedExecutionException e) {
				LogUtil.exception(LOG, Level.TRACE, e, MSG_SUBMIT_REJECTED);
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.TRACE, e, MSG_SUBMIT_FAILED);
			} catch(
				final InstantiationException | IllegalAccessException | InvocationTargetException e
			) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to instantiate the data item");
				break;
			} catch(final InterruptedException e) {
				LOG.debug(LogUtil.MSG, MSG_INTERRUPTED);
				break;
			}
		}
		LOG.debug(LogUtil.MSG, "Finished, generated {} items", i);
	}
	//
}
