package com.emc.mongoose.common.collections;
// mongoose-common.jar
import com.emc.mongoose.common.logging.LogUtil;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.concurrent.ConcurrentLinkedQueue;
/**
 Created by andrey on 09.06.14.
 A pool for any reusable objects(instances).
 Pooled objects should define "release()" method which will invoke the "release" method putting the object(instance) back into a pool.
 Such instances pool may improve the performance in some cases.
 */
public final class InstancePool<T extends Reusable>
extends ConcurrentLinkedQueue<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Class<T> instanceCls;
	//
	public InstancePool(final Class<T> instanceCls) {
		this.instanceCls = instanceCls;
	}
	//
	@SuppressWarnings("unchecked")
	public final T take(final Object... args)
	throws IllegalStateException, IllegalArgumentException {
		T instance = poll();
		if(instance == null) {
			try {
				instance = instanceCls.newInstance();
			} catch(
				final NullPointerException | InstantiationException | IllegalAccessException e
			) {
				throw new IllegalStateException("Reusable instantiation failure", e);
			}
		}
		//
		return (T) instance.reuse(args);
	}
	//
	public final void release(final T instance) {
		if(instance != null) {
			if(!offer(instance)) {
				LOG.debug(
					LogUtil.ERR, "Failed to return the instance \"{}\" back into the pool \"{}\"",
					instance.hashCode(), toString()
				);
			}
		}
	}
	//
	@Override
	public final String toString() {
		return String.format(
			LogUtil.LOCALE_DEFAULT, "%s: %d instances are in the pool",
			instanceCls.getCanonicalName(), size()
		);
	}
}
