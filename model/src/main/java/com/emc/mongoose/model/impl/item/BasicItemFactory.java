package com.emc.mongoose.model.impl.item;

import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.api.item.ItemFactory;

/**
 Created by kurila on 14.07.16.
 */
public final class BasicItemFactory
implements ItemFactory<BasicItem> {
	
	@Override
	public final BasicItem getInstance(
		final String path, final String name, final long id, final long size,
		final ContentSource contentSrc
	) {
		return new BasicItem(path, name);
	}
}
