package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.common.collection.ListingLRUMap;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.ObjectContainerMock;
import org.apache.commons.collections4.map.AbstractLinkedMap;

import java.util.Collection;

/**
 Created on 20.07.16.
 */
public class BasicObjectContainerMock<T extends MutableDataItemMock>
implements ObjectContainerMock<T> {

	private final ListingLRUMap<String, T> containerMap;

	BasicObjectContainerMock(final int capacity) {
		this.containerMap = new ListingLRUMap<String, T>(capacity) {
			@SuppressWarnings("unchecked")
			@Override
			protected boolean removeLRU(
				final LinkEntry entry
			) {
				if(super.removeLRU(entry)) {
					decrementSize();
					return true;
				} else {
					return false;
				}
			}
		};
	}

	@Override
	public int size() {
		return containerMap.size();
	}

	@Override
	public T list(final String afterObjectId, final Collection<T> outputBuffer, final int limit) {
		return containerMap.list(afterObjectId, outputBuffer, limit);
	}

	@Override
	public Collection<T> values() {
		return containerMap.values();
	}

	@Override
	public T get(final String key) {
		return containerMap.get(key);
	}

	@Override
	public T put(final String key, final T value) {
		return containerMap.put(key, value);
	}

	@Override
	public T remove(final String key) {
		return containerMap.remove(key);
	}
}
