package com.emc.mongoose.core.impl.data.util;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import java.io.IOException;
import java.util.Collection;
/**
 The data items input which may be read infinitely (if underlying collection is not empty).
 */
public class CircularCollectionItemInput<T extends DataItem>
extends CollectionItemInput<T> {
	/**
	 @param dataItems the source data items collection
	 */
	public CircularCollectionItemInput(final Collection<T> dataItems) {
		super(dataItems);
	}
	/**
	 @return null if is empty, next data item otherwise
	 @throws java.io.IOException doesn't throw
	 */
	@Override
	public T read()
	throws IOException {
		final T item = super.read();
		if(item == null) {
			reset();
		}
		return item == null ? super.read() : item;
	}
}
