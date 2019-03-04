package com.emc.mongoose.base.item;

/** Created by kurila on 21.09.16. */
public class DataItemFactoryImpl<I extends DataItem> extends ItemFactoryImpl<I>
				implements DataItemFactory<I> {

	@Override
	public I getItem(final String name, final long id, final long size) {
		return (I) new DataItemImpl(name, id, size);
	}

	@Override
	public I getItem(final String line) {
		return (I) new DataItemImpl(line);
	}

	@Override
	public Class<I> getItemClass() {
		return (Class<I>) DataItemImpl.class;
	}
}
