package com.emc.mongoose.api.model.item;

import java.io.IOException;

/**
 Created by kurila on 30.01.17.
 */
public class BasicPathItemFactory<I extends PathItem>
implements ItemFactory<I> {
	
	@Override
	public I getItem(final String name, final long id, final long size) {
		return (I) new BasicPathItem(name);
	}
	
	@Override
	public I getItem(final String line) {
		return (I) new BasicPathItem(line);
	}
	
	@Override
	public Class<I> getItemClass() {
		return (Class<I>) BasicPathItem.class;
	}
	
	@Override
	public void close()
	throws IOException {
	}
}
