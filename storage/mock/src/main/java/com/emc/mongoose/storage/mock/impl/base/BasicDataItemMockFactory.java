package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.item.BasicDataItemFactory;
import com.emc.mongoose.model.item.DataItemFactory;
import com.emc.mongoose.storage.mock.api.DataItemMock;

/**
 Created by kurila on 21.09.16.
 */
public class BasicDataItemMockFactory<I extends DataItemMock>
extends BasicDataItemFactory<I>
implements DataItemFactory<I> {
	
	public BasicDataItemMockFactory(final ContentSource contentSrc) {
		super(contentSrc);
	}
	
	@Override
	public final I getItem(final String name, final long id, final long size) {
		return (I) new BasicDataItemMock(name, id, size, getContentSource());
	}
	
	@Override
	public final I getItem(final String line) {
		return (I) new BasicDataItemMock(line, getContentSource());
	}
	
	@Override
	public final Class<I> getItemClass() {
		return (Class<I>) BasicDataItemMock.class;
	}
}
