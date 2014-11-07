package com.emc.mongoose.util.pool;
//
import com.emc.mongoose.util.logging.Markers;
//
import com.emc.mongoose.util.logging.MessageFactoryImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.Closeable;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
/**
 Created by andrey on 09.06.14.
 A pool for any reusable objects(instances).
 Pooled objects should define "close()" method which will invoke "release" method putting the object(instance) back into a pool.
 Such instances pool may improve the performance in some cases.
 */
public final class BasicInstancePool<T extends Closeable> {
	//
	private static volatile Logger LOG = LogManager.getLogger();
	public static void setLogger(final Logger log) {
		LOG = log;
	}
	//
	private final Set<T> availItems = Collections.synchronizedSet(new HashSet<T>());
	private final Class<T> itemCls;
	//
	public BasicInstancePool(final Class<T> itemCls) {
		this.itemCls = itemCls;
	}
	//
	public final synchronized T take() {
		T item = null;
		if(availItems.isEmpty()) {
			try {
				item = itemCls.newInstance();
			} catch(final NullPointerException|InstantiationException|IllegalAccessException e) {
				LOG.error(
					Markers.ERR,
					"Failed to instantiate pool object, check that default constructor exists", e
				);
			}
		} else {
			try {
				item = availItems.iterator().next();
				availItems.remove(item);
			} catch(final ConcurrentModificationException|NoSuchElementException e) {
				LOG.error(Markers.ERR, "Unexpected exception:", e);
			}
		}
		return item;
	}
	//
	public final synchronized void release(final T item) {
		try {
			availItems.add(item);
		} catch(final ConcurrentModificationException e) {
			LOG.error(Markers.ERR, "Unexpected exception:", e);
		}
	}
}
