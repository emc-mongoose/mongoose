package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.core.api.data.model.DataItemOutput;
//
import java.io.IOException;
import java.util.List;
/**
 Created by kurila on 18.06.15.
 Writable collection of the data items.
 */
public class ListItemOutput<T extends DataItem>
implements DataItemOutput<T> {
	//
	protected final List<T> items;
	//
	public ListItemOutput(final List<T> items) {
		this.items = items;
	}

	/**
	 @param dataItem the data item to write
	 @throws IOException if the destination collection fails to add the data item
	 (due to capacity reasons for example)
	 */
	@Override
	public void write(final T dataItem)
	throws IOException {
		if(!items.add(dataItem)) {
			throw new IOException("Failed to add the data item to the destination collection");
		}
	}
	/**
	 Bulk write of the data items from the specified buffer
	 @param buffer the buffer containing the data items to write
	 @return the count of the data items which have been written successfully
	 @throws IOException doesn't throw
	 */
	@Override
	public int write(final List<T> buffer)
	throws IOException {
		final int n = items.size();
		items.addAll(buffer);
		return items.size() - n;
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
	protected void finalize()
	throws Throwable {
		items.clear();
		super.finalize();
	}
}
