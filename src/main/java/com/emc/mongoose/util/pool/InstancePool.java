package com.emc.mongoose.util.pool;
//
import com.emc.mongoose.util.logging.ExceptionHandler;
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
 Pooled objects should define "close()" method which will invoke "release" method putting the object(instance) back into a pool.
 Such instances pool may improve the performance in some cases.
 */
public final class InstancePool<T extends Reusable>
extends TreeSet<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static class InstanceUsageStats {
		private final static String FMT = "taken/released: %d/%d times";
		private final AtomicLong
			countTaken = new AtomicLong(0),
			countReleased = new AtomicLong(0);
		protected final void taken() {
			countTaken.incrementAndGet();
		}
		protected final void released() {
			countReleased.incrementAndGet();
		}
		@Override
		public final String toString() {
			return String.format(FMT, countTaken.get(), countReleased.get());
		}
	}
	public final static Map<Class, InstancePool> USAGE_STATS = new ConcurrentHashMap<>();
	//
	private final Class<T> itemCls;
	private final Map<Integer, InstanceUsageStats> instanceUsageMap = new ConcurrentHashMap<>();
	//
	public InstancePool(final Class<T> itemCls) {
		this.itemCls = itemCls;
		USAGE_STATS.put(itemCls, this);
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
		if(!instanceUsageMap.containsKey(item.hashCode())) {
			instanceUsageMap.put(item.hashCode(), new InstanceUsageStats());
		}
		instanceUsageMap.get(item.hashCode()).taken();
		//
		return (T) item.reuse(args);
	}
	//
	public final synchronized void release(final T item) {
		if(item != null) {
			if(!add(item)) {
				LOG.debug(
					Markers.ERR, "Releasing already existing in the pool instance: {}",
					item.hashCode()
				);
			} else {
				instanceUsageMap.get(item.hashCode()).released();
			}
		}
	}
	//
	@SuppressWarnings("unchecked")
	public static void dumpStats() {
		Set<Integer> poolInstances;
		InstancePool instancePool;
		for(final Class cls : USAGE_STATS.keySet()) {
			instancePool = USAGE_STATS.get(cls);
			poolInstances = (Set<Integer>) instancePool.instanceUsageMap.keySet();
			LOG.debug(
				Markers.INSTANCE_POOL_STATS, "\"{}\": instance count: {}",
				instancePool.itemCls.getCanonicalName(), poolInstances.size()
			);
			for(final Integer hashCode : poolInstances) {
				LOG.debug(
					Markers.INSTANCE_POOL_STATS, "\t{}: {}",
					hashCode, instancePool.instanceUsageMap.get(hashCode).toString()
				);
			}
		}
	}
}
