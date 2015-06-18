package com.emc.mongoose.util.client.impl;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import java.io.IOException;
import java.util.Collection;
/**
 Created by kurila on 18.06.15.
 */
public class CircularCollectionItemInput<T extends DataItem>
extends CollectionItemInput<T> {
	//
	public CircularCollectionItemInput(final Collection<T> dataItems) {
		super(dataItems);
	}
	/**
	 @return null if the list is empty
	 @throws java.io.IOException doesn't throw
	 */
	@Override
	public T read()
	throws IOException {
		if(itemsIter == null || !itemsIter.hasNext()) {
			reset();
		}
		return super.read();
	}
}
