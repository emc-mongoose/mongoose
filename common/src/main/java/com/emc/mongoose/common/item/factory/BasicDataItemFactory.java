package com.emc.mongoose.common.item.factory;

import com.emc.mongoose.common.data.ContentSource;
import com.emc.mongoose.common.item.BasicDataItem;

/**
 Created by kurila on 14.07.16.
 */
public final class BasicDataItemFactory
implements ItemFactory<BasicDataItem> {
	
	@Override
	public final BasicDataItem getInstance(
		final String path, final String name, final long id, final long size,
		final ContentSource contentSrc
	) {
		return new BasicDataItem(path, name, id, size, contentSrc);
	}
}
