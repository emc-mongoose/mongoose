package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.common.collection.ListingLRUMap;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.ObjectContainerMock;
import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.map.AbstractLinkedMap;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 Created on 20.07.16.
 */
public class BasicObjectContainerMock<T extends MutableDataItemMock>
implements ObjectContainerMock<T> {

	private final ListingLRUMap<String, T> containerMap;

	public BasicObjectContainerMock(final int capacity) {
		this.containerMap = new ListingLRUMap<>(capacity);
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
