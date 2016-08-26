package com.emc.mongoose.model.impl.item;

import com.emc.mongoose.model.api.io.Input;
import com.emc.mongoose.model.api.item.Item;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;

/**
 Readable collection of the data items.
 */
public class ListItemInput<T extends Item>
implements Input<T> {
	//
	//
	protected final List<T> items;
	protected volatile int i = 0;
	//
	public ListItemInput(final List<T> items) {
		this.items = items;
	}

	/**
	 @return next data item
	 @throws EOFException if there's nothing to get more
	 @throws IOException doesn't throw
	 */
	@Override
	public T get()
	throws IOException {
		if(i < items.size()) {
			return items.get(i++);
		} else {
			throw new EOFException();
		}
	}

	/**
	 Bulk get into the specified buffer
	 @param buffer buffer for the data items
	 @param maxCount the count limit
	 @return the count of the data items been get
	 @throws EOFException if there's nothing to get more
	 @throws IOException if fails some-why
	 */
	@Override
	public int get(final List<T> buffer, final int maxCount)
	throws IOException {
		int n = items.size() - i;
		if(n > 0) {
			n = Math.min(n, maxCount);
			buffer.addAll(items.subList(i, i + n));
		} else {
			throw new EOFException();
		}
		i += n;
		return n;
	}

	/**
	 @throws IOException doesn't throw
	 */
	@Override
	public void reset()
	throws IOException {
		i = 0;
	}

	@Override
	public void skip(final long itemsCount)
	throws IOException {
		if(items.size() < itemsCount)
			throw new IOException();
		i = (int) itemsCount;
	}

	/**
	 Does nothing
	 @throws IOException doesn't throw
	 */
	@Override
	public void close()
	throws IOException {
	}

	@Override
	public String toString() {
		return "listItemInput<" + items.hashCode() + ">";
	}
}
