package com.emc.mongoose.common.item.factory;

import com.emc.mongoose.common.data.ContentSource;
import com.emc.mongoose.common.item.BasicItem;

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
