package com.emc.mongoose.pool;
//
import com.emc.mongoose.logging.Markers;
//
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
 */
public final class GenericInstancePool<T extends Closeable> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Set<T> availItems = Collections.synchronizedSet(new HashSet<T>());
	private final Class<T> itemCls;
	//
	public GenericInstancePool(final Class<T> itemCls) {
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
