package com.emc.mongoose.core.impl.data.util;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import java.io.EOFException;
import java.io.IOException;
import java.util.List;
/**
 The data items input which may be read infinitely (if underlying collection allows).
 */
public class CircularListItemInput<T extends DataItem>
extends ListItemInput<T> {
	/**
	 @param dataItems the source data items collection
	 */
	public CircularListItemInput(final List<T> dataItems) {
		super(dataItems);
	}

	/**
	 @return next data item
	 */
	@Override
	public T read()
	throws IOException {
		try {
			return super.read();
		} catch(final EOFException e) {
			reset();
			return super.read();
		}
	}
}
