package com.emc.mongoose.base.item;

/** Created by kurila on 30.01.17. */
public class PathItemFactoryImpl<I extends PathItem> implements ItemFactory<I> {

	@Override
	public I getItem(final String name, final long id, final long size) {
		return (I) new PathItemImpl(name);
	}

	@Override
	public I getItem(final String line) {
		return (I) new PathItemImpl(line);
	}

	@Override
	public Class<I> getItemClass() {
		return (Class<I>) PathItemImpl.class;
	}
}
