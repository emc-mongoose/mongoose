package com.emc.mongoose.core.api.data.model;
//
import com.emc.mongoose.core.api.Item;
//
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;

/**
 Created by kurila on 17.06.15.
 */
public interface ItemSrc<T extends Item>
extends Closeable {

	String
		MSG_SKIP_START = "Skipping {} items. This may take some time to complete. Please wait...",
		MSG_SKIP_END = "Items have been skipped";
	/**
	 * Set last processed item.
	 * @param lastItem last processed item
	 */
	void setLastItem(final T lastItem);

	/**
	 * Get last processed item
	 * @return last processed item
	 */
	T getLastItem();

	/**
	 Get next item
	 @return next item or null if no items available more
	 @throws java.io.EOFException if no item available more
	 @throws java.io.IOException if failed to get some-why
	 */
	T get()
	throws EOFException, IOException;

	/**
	 Bulk items get.
	 @param buffer buffer for the items
	 @param limit max count of the items to put into the buffer
	 @return count of the items have been get and put into the buffer actually
	 @throws java.io.EOFException if no item available more
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
	 * Skip some items.
	 * @param itemsCount count of items should be skipped from the input stream
	 * @throws IOException if failed to skip such amount of bytes
	 */
	void skip(final long itemsCount)
	throws IOException;

}
