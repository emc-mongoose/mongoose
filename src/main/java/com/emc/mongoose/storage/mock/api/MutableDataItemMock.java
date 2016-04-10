package com.emc.mongoose.storage.mock.api;
//
import com.emc.mongoose.core.api.v1.item.data.MutableDataItem;
/**
 Created by kurila on 27.07.15.
 */
public interface MutableDataItemMock
extends MutableDataItem {
	void update(final long offset, final long size);
	void append(final long offset, final long size);
}
