package com.emc.mongoose.model.impl.item;

import com.emc.mongoose.model.api.io.Output;
import com.emc.mongoose.model.api.item.Item;

import java.io.IOException;
import java.util.List;
/**
 Created by kurila on 18.06.15.
 Writable collection of the data items.
 */
public class ListItemOutput<T extends Item>
implements Output<T> {
	//
	protected final List<T> items;
	//
	public ListItemOutput(final List<T> items) {
		this.items = items;
	}

	/**
	 @param item the data item to put
	 @throws IOException if the destination collection fails to add the data item
	 (due to capacity reasons for example)
	 */
	@Override
	public void put(final T item)
	throws IOException {
		if(!items.add(item)) {
			throw new IOException("Failed to add the data item to the destination collection");
		}
	}
	/**
	 Bulk put of the data items from the specified buffer
	 @param buffer the buffer containing the data items to put
	 @return the count of the data items which have been written successfully
	 @throws IOException doesn't throw
	 */
	@Override
	public int put(final List<T> buffer, final int from, final int to)
	throws IOException {
		items.addAll(buffer.subList(from, to));
		return to - from;
	}

	//
	@Override
	public final int put(final List<T> items)
	throws IOException {
		return put(items, 0, items.size());
	}

	/**
	 @return the corresponding input
	 @throws IOException doesn't throw
	 */
	@Override
	public ListItemInput<T> getInput()
	throws IOException {
		return new ListItemInput<>(items);
	}

	/**
	 does nothing
	 @throws IOException doesn't throw
	 */
	@Override
	public void close()
	throws IOException {
	}

	//
	@Override
	public String toString() {
		return "listItemOutput<" + items.hashCode() + ">";
	}

}
