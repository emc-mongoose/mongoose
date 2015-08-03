package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.storage.mock.api.DataObjectMock;
import com.emc.mongoose.storage.mock.api.ObjectContainerMock;
//
import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
/**
 Created by kurila on 31.07.15.
 */
public class BasicObjectContainerMock<T extends DataObjectMock>
extends ConcurrentSkipListMap<String, T>
implements ObjectContainerMock<T> {
	//
	protected final String name;
	protected final int capacity;
	protected final AtomicInteger size = new AtomicInteger(0);
	//
	public BasicObjectContainerMock(final String name, final int capacity) {
		super();
		this.name = name;
		this.capacity = capacity;
	}
	//
	@Override
	public final String getName() {
		return name;
	}
	//
	@Override
	public final String list(final String marker, final Collection<T> buffDst, final int maxCount) {
		String nextId = marker;
		for(int i = 0; i < maxCount; i ++) {
			nextId = nextId == null ? firstKey() : higherKey(nextId);
			if(nextId == null) {
				break;
			}
			buffDst.add(get(nextId));
		}
		return nextId;
	}
	//
	@Override
	public final T remove(final Object key) {
		final T removed = super.remove(key);
		if(removed != null) {
			size.decrementAndGet();
		}
		return removed;
	}
	//
	@Override
	public final T put(final String id, final T object) {
		final T replaced = super.put(id, object);
		if(replaced == null) {
			if(size.get() == capacity) {
				super.remove(lastKey());
			} else {
				size.incrementAndGet();
			}
		}
		return replaced;
	}
}
