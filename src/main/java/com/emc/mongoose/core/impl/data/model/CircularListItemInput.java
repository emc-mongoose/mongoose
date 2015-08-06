package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;
//
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
		if(i >= items.size()) {
			reset();
		}
		return items.get(i ++);
	}

	@Override
	public String toString() {
		return "circularListItemInput<" + items.toString() + ">";
	}
}
