package com.emc.mongoose.common.item.factory;

import com.emc.mongoose.common.data.ContentSource;
import com.emc.mongoose.common.item.Item;

/**
 Created by kurila on 14.07.16.
 */
public interface ItemFactory<I extends Item> {
	I getInstance(
		final String path, final String name, final long id, final long size,
		final ContentSource contentSrc
	);
}
