package com.emc.mongoose.base.item;

/** Created by kurila on 30.01.17. */
public class TokenItemFactoryImpl<I extends TokenItem> implements ItemFactory<I> {

	@Override
	public I getItem(final String name, final long id, final long size) {
		return (I) new TokenItemImpl(name);
	}

	@Override
	public I getItem(final String line) {
		return (I) new TokenItemImpl(line);
	}

	@Override
	public Class<I> getItemClass() {
		return (Class<I>) TokenItemImpl.class;
	}
}
