package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
/**
 The data items input which may be written infinitely (if underlying collection allows that).
 */
public class CircularListItemOutput<T extends DataItem>
extends ListItemOutput<T> {
	//
	protected int capacity, i = 0;
	//
	public CircularListItemOutput(final List<T> itemList, final int capacity)
	throws IllegalArgumentException {
		super(itemList);
		if(ArrayList.class.isInstance(itemList)) {
			ArrayList.class.cast(itemList).ensureCapacity(capacity);
		}
		if(capacity < 1) {
			throw new IllegalArgumentException("Capacity should be > 0");
		}
		this.capacity = capacity;
	}
	/**
	 @param dataItem the data item to write
	 @throws java.io.IOException if the destination collection fails to add the data item
	 */
	@Override
	public void write(final T dataItem)
	throws IOException {
		if(items.size() < capacity) {
			super.write(dataItem);
		} else {
			if(i >= capacity) {
				i = 0;
			}
			items.set(i, dataItem);
		}
		i ++;
	}
	/**
	 Bulk circular write method
	 @param buffer the list of the items to write in a batch mode
	 @throws java.io.IOException if the destination collection fails to add the data items
	 @return the size of the buffer to write
	 */
	@Override
	public int write(final List<T> buffer)
	throws IOException {
		final int bufferSize = buffer.size();
		if(bufferSize < capacity) {
			// buffer may be placed entirely into the capacitor
			final int limit = capacity - items.size(); // how many free space is in the capacitor;
			if(bufferSize > limit) {
				// should remove some items from the beginning of the capacitor in order to place
				// the buffer entirely
				items.removeAll(items.subList(0, bufferSize - limit));
			}
			if(!items.addAll(buffer)) {
				throw new IOException("Failed to write " + bufferSize + " items");
			}
		} else {
			// only a tail part of the buffer may be placed into the capacitor
			items.clear(); // discard all the items in the capacitor
			if(!items.addAll(buffer.subList(bufferSize - capacity, bufferSize))) {
				throw new IOException("Failed to write " + bufferSize + " items");
			}
		}
		return bufferSize;
	}
	/**
	 @return the corresponding input
	 @throws IOException doesn't throw
	 */
	@Override
	public CircularListItemInput<T> getInput()
	throws IOException {
		return new CircularListItemInput<>(items);
	}
}
