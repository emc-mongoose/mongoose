package com.emc.mongoose.model.impl.item;

import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.item.ItemFactory;

/**
 Created by kurila on 14.07.16.
 */
public class BasicItemFactory<I extends Item>
implements ItemFactory<I> {
	
	@Override
	public I getItem(final String path, final String name, final long id, final long size) {
		return (I) new BasicItem(path, name);
	}
	
	@Override
	public I getItem(final String line) {
		return (I) new BasicItem(line);
	}
	
	@Override
	public Class<I> getItemClass() {
		return (Class<I>) BasicItem.class;
	}
}
