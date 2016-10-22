package com.emc.mongoose.model.impl.item;

import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.api.item.ItemFactory;

/**
 Created by kurila on 21.09.16.
 */
public class BasicDataItemFactory<I extends DataItem>
extends BasicItemFactory<I>
implements ItemFactory<I> {
	
	protected final ContentSource contentSrc;
	
	public BasicDataItemFactory(final ContentSource contentSrc) {
		this.contentSrc = contentSrc;
	}
	
	@Override
	public I getItem(final String name, final long id, final long size) {
		return (I) new BasicDataItem(name, id, size, contentSrc);
	}
	
	@Override
	public I getItem(final String line) {
		return (I) new BasicDataItem(line, contentSrc);
	}
	
	@Override
	public Class<I> getItemClass() {
		return (Class<I>) BasicDataItem.class;
	}
}
