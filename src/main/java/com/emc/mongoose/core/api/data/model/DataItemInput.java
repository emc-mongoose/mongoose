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
public interface DataItemInput<T extends DataItem>
extends Closeable {

	/**
	 * Set id of last processed data item.
	 * @param lastItemId if of last processed data item
	 */
	void setLastItemId(final String lastItemId);

	/**
	 * Get id of last processed data item.
	 * @return id of last processed data item
	 */
	String getLastItemId();
	/**
	 Get next data item
	 @return next data item or null if no data item available
	 @throws java.io.EOFException if no data item available more
	 @throws java.io.IOException if failed to read some-why
	 */
	T read()
	throws EOFException, IOException;

	/**
	 Bulk data items read.
	 @param buffer buffer for the data items
	 @param maxCount max count of the items to read
	 @return count of the data items have been read and put into the buffer actually
	 @throws java.io.EOFException if no data item available more
	 @throws java.io.IOException if failed to read some-why
	 */
	int read(final List<T> buffer, final int maxCount)
	throws IOException;

	/**
	 Reset this input making this readable from the beginning
	 */
	void reset()
	throws IOException;

	/**
	 * Skip some data items.
	 * @param countOfItems count of items which should be skipped from the beginning
	 * @throws IOException if failed to skip such amount of data items
	 */
	void skip(final long countOfItems)
	throws IOException;
}
