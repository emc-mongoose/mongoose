package com.emc.mongoose.common.io.collection;

import com.emc.mongoose.common.io.Input;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;

/**
 Readable collection of the data items. Not thread safe.
 */
public class ListInput<T>
implements Input<T> {
	
	protected final List<T> items;
	protected int size;
	protected int i = 0;
	
	public ListInput(final List<T> items) {
		this.items = items;
		this.size = items == null ? 0 : items.size();
	}

	/**
	 @return next data item
	 @throws EOFException if there's nothing to get more
	 */
	@Override
	public T get()
	throws EOFException, IOException {
		if(i < size) {
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
	 */
	@Override
	public int get(final List<T> buffer, final int maxCount)
	throws EOFException, IOException {
		int n = size - i;
		if(n > 0) {
			n = Math.min(n, maxCount);
			for(final T item : items.subList(i, i + n)) {
				buffer.add(item);
			}
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
	public long skip(final long itemsCount)
	throws IOException {
		final int remainingCount = size - i;
		if(itemsCount > remainingCount) {
			i = 0;
			return remainingCount;
		} else {
			i += (int) itemsCount;
			return itemsCount;
		}
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
