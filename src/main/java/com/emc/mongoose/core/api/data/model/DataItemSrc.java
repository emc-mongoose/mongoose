package com.emc.mongoose.core.api.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;

/**
 Created by kurila on 17.06.15.
 */
public interface DataItemSrc<T extends DataItem>
extends Closeable {

	String
		MSG_SKIP_START = "Skipping {} data items. " +
			"This may take some time to complete. Please wait...",
		MSG_SKIP_END = "Items have been skipped";
	/**
	 * Set last processed data item.
	 * @param lastItem last processed data item
	 */
	void setLastDataItem(final T lastItem);

	/**
	 * Get last processed data item
	 * @return last processed data item
	 */
	DataItem getLastDataItem();
	/**
	 Get next data item
	 @return next data item or null if no data item available
	 @throws java.io.EOFException if no data item available more
	 @throws java.io.IOException if failed to get some-why
	 */
	T get()
	throws EOFException, IOException;

	/**
	 Bulk data items get.
	 @param buffer buffer for the data items
	 @param limit max count of the items to put into the buffer
	 @return count of the data items have been get and put into the buffer actually
	 @throws java.io.EOFException if no data item available more
	 @throws java.io.IOException if failed to get some-why
	 */
	int get(final List<T> buffer, final int limit)
	throws IOException;

	/**
	 Reset this input making this readable from the beginning
	 */
	void reset()
	throws IOException;

	/**
	 * Skip some data items.
	 * @param itemsCount count of bytes should be skipped from the input stream
	 * @throws IOException if failed to skip such amount of bytes
	 */
	void skip(final long itemsCount)
	throws IOException;

}
