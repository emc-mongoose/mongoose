package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.core.api.data.model.DataItemInput;
//
import java.io.EOFException;
import java.io.IOException;
import java.util.List;
/**
 Readable collection of the data items.
 */
public class ListItemInput<T extends DataItem>
implements DataItemInput<T> {
	//
	protected final List<T> items;
	protected int i = 0;
	//
	public ListItemInput(final List<T> items) {
		this.items = items;
	}

	/**
	 @return next data item
	 @throws java.io.EOFException if there's nothing to read more
	 @throws java.io.IOException doesn't throw
	 */
	@Override
	public T read()
	throws IOException {
		if(i < items.size()) {
			return items.get(i ++);
		} else {
			throw new EOFException();
		}
	}

	/**
	 Bulk read into the specified buffer
	 @param buffer buffer for the data items
	 @param maxCount the count limit
	 @return the count of the data items been read
	 @throws java.io.EOFException if there's nothing to read more
	 @throws IOException if fails some-why
	 */
	@Override
	public int read(final List<T> buffer, final int maxCount)
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
		return "listItemInput<" + items.toString() + ">";
	}
}
