package com.emc.mongoose.util.pool;
//
import com.emc.mongoose.util.logging.ExceptionHandler;
//
import com.emc.mongoose.util.logging.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.TreeSet;
/**
 Created by andrey on 09.06.14.
 A pool for any reusable objects(instances).
 Pooled objects should define "close()" method which will invoke "release" method putting the object(instance) back into a pool.
 Such instances pool may improve the performance in some cases.
 */
public final class InstancePool<T extends Reusable>
extends TreeSet<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Class<T> itemCls;
	//
	public InstancePool(final Class<T> itemCls) {
		this.itemCls = itemCls;
	}
	//
	@SuppressWarnings("unchecked")
	public final synchronized T take(final Object... args)
	throws IllegalStateException {
		T item = null;
		if(isEmpty()) {
			try {
				item = itemCls.newInstance();
			} catch(final NullPointerException|InstantiationException|IllegalAccessException e) {
				ExceptionHandler.trace(LOG, Level.ERROR, e, "Reusable instantiation failure");
			}
		} else {
			item = first();
		}
		//
		if(item == null) {
			throw new IllegalStateException("No instance was taken");
		}
		remove(item);
		//
		return (T) item.reuse(args);
	}
	//
	public final synchronized void release(final T item) {
		if(item != null && !add(item) && LOG.isTraceEnabled(Markers.ERR)) {
			LOG.trace(
				Markers.ERR, "Releasing already existing in the pool instance: {}", item.hashCode()
			);
		}
	}
}
