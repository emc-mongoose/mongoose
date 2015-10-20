package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.core.api.Item;
//
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
/**
 The data items input which may be written infinitely (if underlying collection allows that).
 */
public class CircularListItemDst<T extends Item>
extends ListItemDst<T> {
	//
	protected int capacity, i = 0;
	//
	public CircularListItemDst(final List<T> itemList, final int capacity)
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
	 @param dataItem the data item to put
	 @throws java.io.IOException if the destination collection fails to add the data item
	 */
	@Override
	public void put(final T dataItem)
	throws IOException {
		if(items.size() < capacity) {
			super.put(dataItem);
		} else {
			if(i >= capacity) {
				i = 0;
			}
			items.set(i, dataItem);
		}
		i ++;
	}
	/**
	 Bulk circular put method
	 @param buffer the list of the items to put in a batch mode
	 @throws java.io.IOException if the destination collection fails to add the data items
	 @return the size of the buffer to put
	 */
	@Override
	public int put(final List<T> buffer, final int from, final int to)
	throws IOException {
		//
		int n = to - from;
		if(buffer.size() > n) {
			return put(buffer.subList(from, to), 0, n);
		}
		//
		n = buffer.size();
		if(n < capacity) {
			// buffer may be placed entirely into the capacitor
			final int limit = capacity - items.size(); // how many free space is in the capacitor;
			if(n > limit) {
				// should remove some items from the beginning of the capacitor in order to place
				// the buffer entirely
				items.removeAll(items.subList(0, n - limit));
			}
			if(!items.addAll(0, buffer)) {
				throw new IOException("Failed to put " + n + " items");
			}
		} else {
			// only a tail part of the buffer may be placed into the capacitor
			items.clear(); // discard all the items in the capacitor
			if(!items.addAll(buffer.subList(n - capacity, n))) {
				throw new IOException("Failed to put " + n + " items");
			}
		}
		return n;
	}
	/**
	 @return the corresponding input
	 @throws IOException doesn't throw
	 */
	@Override
	public CircularListItemSrc<T> getItemSrc()
	throws IOException {
		return new CircularListItemSrc<>(items);
	}
	//
	@Override
	public final String toString() {
		return "circularItemOutput<" + items.hashCode() + ">";
	}
}
