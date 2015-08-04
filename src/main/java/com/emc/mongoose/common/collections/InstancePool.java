package com.emc.mongoose.common.collections;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
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
	private final Constructor<T> constructor;
	private final Object sharedArgs[];
	private final AtomicInteger instCount = new AtomicInteger(0);
	//
	public InstancePool(final Class<T> cls) {
		Constructor<T> constr = null;
		try {
			constr = cls.getConstructor();
		} catch(final NoSuchMethodException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to get the default constructor for class {}", cls
			);
		}
		constructor = constr;
		sharedArgs = null;
	}
	//
	public InstancePool(final Constructor<T> constructor, final Object... sharedArgs) {
		this.constructor = constructor;
		this.sharedArgs = sharedArgs;
	}
	//
	@SuppressWarnings("unchecked")
	public final T take(final Object... args)
	throws IllegalStateException, IllegalArgumentException {
		T instance = poll();
		if(instance == null) {
			try {
				if(sharedArgs == null || sharedArgs.length == 0) {
					instance = constructor.newInstance();
					instCount.incrementAndGet();
				} else if(sharedArgs.length == 1) {
					instance = constructor.newInstance(sharedArgs[0]);
					instCount.incrementAndGet();
				} else {
					throw new IllegalArgumentException("Not implemented");
				}
			} catch(
				final InstantiationException | IllegalAccessException | InvocationTargetException e
			) {
				throw new IllegalStateException("Reusable instantiation failure", e);
			}
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "Using new instance: {}/{}", instance.hashCode(), instance);
			}
		} else {
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "Reusing the instance: {}/{}", instance.hashCode(), instance);
			}
		}
		//
		return (T) instance.reuse(args);
	}
	//
	public final void release(final T instance) {
		if(instance != null) {
			if(offer(instance)) {
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(
						Markers.MSG, "Released the instance: {}/{}", instance.hashCode(), instance
					);
				}
			} else {
				LOG.debug(
					Markers.ERR, "Failed to return the instance \"{}\" back into the pool \"{}\"",
					instance.hashCode(), toString()
				);
			}
		}
	}
	//
	@Override
	public final String toString() {
		return "InstancePool<" + constructor.getDeclaringClass().getCanonicalName() + ">: " +
			size() + " instances are in the pool of " + instCount.get() + " total";
	}
}
