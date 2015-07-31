package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.storage.mock.api.DataObjectMock;
import com.emc.mongoose.storage.mock.api.ObjectContainerMock;
//
import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListMap;
/**
 Created by kurila on 31.07.15.
 */
public class BasicObjectContainerMock<T extends DataObjectMock>
extends ConcurrentSkipListMap<String, T>
implements ObjectContainerMock<T> {
	//
	protected final String name;
	protected final int capacity;
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
			nextId = higherKey(nextId);
			if(nextId == null) {
				break;
			}
			buffDst.add(get(nextId));
		}
		return nextId;
	}
	//
	@Override
	public final T put(final String id, final T object) {
		if(size() == capacity) {
			super.remove(lastKey());
		}
		return super.put(id, object);
	}
}
