package com.emc.mongoose.api.model.item;

/**
 Created by kurila on 21.09.16.
 */
public class BasicDataItemFactory<I extends DataItem>
extends BasicItemFactory<I>
implements DataItemFactory<I> {

	@Override
	public I getItem(final String name, final long id, final long size) {
		return (I) new BasicDataItem(name, id, size);
	}
	
	@Override
	public I getItem(final String line) {
		return (I) new BasicDataItem(line);
	}

	@Override
	public Class<I> getItemClass() {
		return (Class<I>) BasicDataItem.class;
	}
}
