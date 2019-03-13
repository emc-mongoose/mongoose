package com.emc.mongoose.base.item;

/** Created by kurila on 14.07.16. */
public class ItemFactoryImpl<I extends Item> implements ItemFactory<I> {

	@Override
	public I getItem(final String name, final long id, final long size) {
		return (I) new ItemImpl(name);
	}

	@Override
	public I getItem(final String line) {
		return (I) new ItemImpl(line);
	}

	@Override
	public Class<I> getItemClass() {
		return (Class<I>) ItemImpl.class;
	}
}
