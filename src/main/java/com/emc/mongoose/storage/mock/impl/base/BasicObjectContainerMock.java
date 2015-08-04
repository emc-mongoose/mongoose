package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.storage.mock.api.DataObjectMock;
import com.emc.mongoose.storage.mock.api.ObjectContainerMock;
//
import org.apache.commons.collections4.map.AbstractLinkedMap;
import org.apache.commons.collections4.map.LRUMap;
//
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/**
 Created by kurila on 31.07.15.
 */
public final class BasicObjectContainerMock<T extends DataObjectMock>
extends LRUMap<String, T>
implements ObjectContainerMock<T> {
	//
	private final String name;
	private final Lock lock = new ReentrantLock();
	//
	public BasicObjectContainerMock(final String name, final int capacity) {
		super(capacity);
		this.name = name;
	}
	//
	@Override
	public final String getName() {
		return name;
	}
	//
	@Override
	public final String list(
		final String marker, final Collection<T> buffDst, final int maxCount
	) {
		AbstractLinkedMap.LinkEntry<String, T> nextEntry;
		lock.lock();
		try {
			nextEntry = getEntry(marker);
			for(int i = 0; i < maxCount; i++) {
				nextEntry = nextEntry == null ? getEntry(firstKey()) : entryAfter(nextEntry);
				if(nextEntry == null) {
					break;
				}
				buffDst.add(nextEntry.getValue());
			}
		} finally {
			lock.unlock();
		}
		return nextEntry == null ? null : nextEntry.getKey();
	}
	//
	@Override
	public final T put(final String oid, final T obj) {
		lock.lock();
		try {
			return super.put(oid, obj);
		} finally {
			lock.unlock();
		}
	}
	//
	@Override
	public final T remove(final Object oid) {
		lock.lock();
		try {
			return super.remove(oid);
		} finally {
			lock.unlock();
		}
	}
	//
	@Override
	protected void finalize()
	throws Throwable {
		clear();
		super.finalize();
	}
}
