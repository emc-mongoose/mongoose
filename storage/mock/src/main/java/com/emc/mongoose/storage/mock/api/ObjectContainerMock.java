package com.emc.mongoose.storage.mock.api;

import com.emc.mongoose.common.collection.Listable;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.api.item.MutableDataItem;

import java.util.Collection;
import java.util.Map;

/**
 Created on 19.07.16.
 */
public interface ObjectContainerMock<T extends MutableDataItem> extends Listable<T> {

	T get (final String key);

	T put(final String key, final T value);

	T remove(final String key);

	int size();

	Collection<T> values();
}
