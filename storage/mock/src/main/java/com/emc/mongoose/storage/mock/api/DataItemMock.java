package com.emc.mongoose.storage.mock.api;

import com.emc.mongoose.model.item.DataItem;

/**
 Created on 19.07.16.
 */
public interface DataItemMock
extends DataItem {

	void update(final long offset, final long size);

	void append(final long size);

}
