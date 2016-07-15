package com.emc.mongoose.model.api.io;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;
/**
 Created by andrey on 06.04.16.
 */
public interface Input<T>
extends Closeable {

	String DELIMITER = ";";
	String MSG_SKIP_START = "Skipping {} items. This may take some time. Please wait...";
	String MSG_SKIP_END = "Items have been skipped";

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
	 * Skip some items.
	 * @param count count of items should be skipped from the input stream
	 * @throws IOException if failed to skip such amount of bytes
	 */
	void skip(final long count)
	throws IOException;

	/**
	 Reset this input making this readable from the beginning
	 */
	void reset()
	throws IOException;
}
