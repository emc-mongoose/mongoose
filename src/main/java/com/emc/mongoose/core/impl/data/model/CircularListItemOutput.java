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
	 */
	@Override
	public int write(final List<T> buffer)
	throws IOException {
		int n, done = 0, limit, bufferSize = buffer.size();
		try {
			do {
				if(items.size() == capacity) {
					i = 0;
				}
				System.out.println(done + ", " + i + ", " + items.size());
				limit = Math.min(bufferSize - done, i == 0 ? capacity : capacity - items.size());
				n = items.size();
				if(!items.addAll(buffer.subList(done, done + limit))) {
					throw new IOException("Failed to write " + limit + " items");
				}
				n = items.size() - n;
				done += n;
				i += n;
			} while(done < bufferSize);
		} catch(final Exception e) {
			e.printStackTrace(System.err);
		}
		return done;
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
