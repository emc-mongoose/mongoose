package com.emc.mongoose.item;

/**
 Created by kurila on 30.01.17.
 */
public class BasicTokenItemFactory<I extends TokenItem>
implements ItemFactory<I> {
	
	@Override
	public I getItem(final String name, final long id, final long size) {
		return (I) new BasicTokenItem(name);
	}
	
	@Override
	public I getItem(final String line) {
		return (I) new BasicTokenItem(line);
	}
	
	@Override
	public Class<I> getItemClass() {
		return (Class<I>) BasicTokenItem.class;
	}
}
