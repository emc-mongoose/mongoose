package com.emc.mongoose.model.api.item;

import com.emc.mongoose.model.api.data.ContentSource;

/**
 Created by kurila on 14.07.16.
 */
public interface ItemFactory<I extends Item> {
	I getInstance(
		final String path, final String name, final long id, final long size,
		final ContentSource contentSrc
	);
}
