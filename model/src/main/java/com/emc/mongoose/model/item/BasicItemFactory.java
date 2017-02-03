package com.emc.mongoose.model.item;

import java.io.IOException;

/**
 Created by kurila on 14.07.16.
 */
public class BasicItemFactory<I extends Item>
implements ItemFactory<I> {
	
	@Override
	public I getItem(final String name, final long id, final long size) {
		return (I) new BasicItem(name);
	}
	
	@Override
	public I getItem(final String line) {
		return (I) new BasicItem(line);
	}
	
	@Override
	public Class<I> getItemClass() {
		return (Class<I>) BasicItem.class;
	}

	@Override
	public void close()
	throws IOException {
	}
}
