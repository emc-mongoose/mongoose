package com.emc.mongoose.core.impl.item.base;
//
import com.emc.mongoose.core.api.item.base.Item;
//
import java.io.IOException;
import java.util.List;
/**
 The data items input which may be get infinitely (if underlying collection allows).
 */
public class CircularListItemSrc<T extends Item>
extends ListItemSrc<T> {
	/**
	 @param dataItems the source data items collection
	 */
	public CircularListItemSrc(final List<T> dataItems) {
		super(dataItems);
	}

	/**
	 @return next data item
	 */
	@Override
	public T get()
	throws IOException {
		if(i >= items.size()) {
			reset();
		}
		return items.get(i ++);
	}

	@Override
	public String toString() {
		return "circularListItemInput<" + items.hashCode() + ">";
	}
}
