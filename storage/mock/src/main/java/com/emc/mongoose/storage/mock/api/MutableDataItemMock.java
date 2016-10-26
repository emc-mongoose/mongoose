package com.emc.mongoose.storage.mock.api;

import com.emc.mongoose.model.item.MutableDataItem;

/**
 Created on 19.07.16.
 */
public interface MutableDataItemMock extends MutableDataItem {

	void update(final long offset, final long size);

	void append(final long offset, final long size);

}
