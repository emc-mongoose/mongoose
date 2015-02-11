package com.emc.mongoose.util.collections;
//
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.logging.TraceLogger;
//
import com.emc.mongoose.util.logging.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by andrey on 09.06.14.
 A pool for any reusable objects(instances).
 Pooled objects should define "release()" method which will invoke the "release" method putting the object(instance) back into a pool.
 Such instances pool may improve the performance in some cases.
 */
public final class InstancePool<T extends Reusable>
extends TreeSet<T> {
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
	public final synchronized T take(final Object... args)
	throws IllegalStateException, IllegalArgumentException, InterruptedException {
		T instance = null;
		if(isEmpty()) {
			try {
				instance = instanceCls.newInstance();
			} catch(final NullPointerException|InstantiationException|IllegalAccessException e) {
				TraceLogger.failure(LOG, Level.ERROR, e, "Reusable instantiation failure");
			}
		} else {
			instance = first();
		}
		//
		if(instance == null) {
			throw new IllegalStateException("No instance was taken");
		}
		remove(instance);
		//
		return (T) instance.reuse(args);
	}
	//
	public final synchronized void release(final T instance) {
		if(instance != null) {
			if(!add(instance)) {
				LOG.debug(
					Markers.ERR, "The \"{}\" already contains instance \"{}\"",
					toString(), instance.hashCode()
				);
			}
		}
	}
	//
	@Override
	public final String toString() {
		return String.format(
			Main.LOCALE_DEFAULT, "%s: %d instances are in the pool",
			instanceCls.getCanonicalName(), size()
		);
	}
}
