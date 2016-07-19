package com.emc.mongoose.storage.mock.api;

import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.api.item.MutableDataItem;

import java.util.Collection;
import java.util.Map;

/**
 Created on 19.07.16.
 */
public interface ObjectContainerMock<T extends MutableDataItem> extends Map<String, T> {

	int size();
	//
	T list(final String afterOid, final Collection<T> buffDst, final int limit);
}
