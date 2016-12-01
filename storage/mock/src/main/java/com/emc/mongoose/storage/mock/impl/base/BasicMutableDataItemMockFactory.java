package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.item.DataItemFactory;
import com.emc.mongoose.model.item.BasicMutableDataItemFactory;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;

/**
 Created by kurila on 21.09.16.
 */
public class BasicMutableDataItemMockFactory<I extends MutableDataItemMock>
extends BasicMutableDataItemFactory<I>
implements DataItemFactory<I> {
	
	public BasicMutableDataItemMockFactory(final ContentSource contentSrc) {
		super(contentSrc);
	}
	
	@Override
	public final I getItem(final String name, final long id, final long size) {
		return (I) new BasicMutableDataItemMock(name, id, size, getContentSource());
	}
	
	@Override
	public final I getItem(final String line) {
		return (I) new BasicMutableDataItemMock(line, getContentSource());
	}
	
	@Override
	public final Class<I> getItemClass() {
		return (Class<I>) BasicMutableDataItemMock.class;
	}
}
