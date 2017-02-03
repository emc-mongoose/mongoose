package com.emc.mongoose.common.collection;

import org.apache.commons.collections4.map.LRUMap;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 Created on 20.07.16.
 */
public class ListingLRUMap<K, V>
extends LRUMap<K, V> implements Listable<V> {

	private final AtomicInteger size = new AtomicInteger(0);

	public ListingLRUMap(final int capacity) {
		super(capacity);
	}

	@Override
	public int size() {
		return size.get();
	}

	@Override
	public V put(final K key, final V value) {
		final V oldValue = super.put(key, value);
		if(null == oldValue) {
			size.incrementAndGet();
		}
		return oldValue;
	}

	@Override
	public V remove(final Object key) {
		final V value = super.remove(key);
		if(value != null) {
			size.decrementAndGet();
		}
		return value;
	}

	@Override
	public V list(final String afterObjectId, final Collection<V> outputBuffer, final int limit) {
		if(isEmpty()) {
			return null;
		}
		LinkEntry<K, V> nextEntry = getEntry(afterObjectId);
		for(int i = 0; i < limit; i++) {
			if(nextEntry == null) {
				nextEntry = getEntry(firstKey());
			} else {
				nextEntry = entryAfter(nextEntry);
			}
			if(nextEntry == null || nextEntry.getKey() == null) {
				break;
			}
			outputBuffer.add(nextEntry.getValue());
		}
		return (nextEntry == null || nextEntry.getKey() == null) ? null : nextEntry.getValue();
	}

	@Override
	protected void moveToMRU(
		final LinkEntry<K, V> entry
	) {
		// disable entry moving to MRU in case of access
		// it's required to make list method (right below) working (keeping the linked list order)
	}

	protected void decrementSize() {
		size.decrementAndGet();
	}
}
