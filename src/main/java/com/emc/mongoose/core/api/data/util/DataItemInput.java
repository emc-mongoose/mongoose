package com.emc.mongoose.core.api.data.util;
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
	 Get next data item
	 @return next data item or null if no data item available
	 @throws java.io.EOFException if no data item available more
	 @throws java.io.IOException if failed to read some-why
	 */
	T read()
	throws EOFException, IOException;

	/**
	 Bulk data items read. Will try to read up to buffer.length data items in a time.
	 @param buffer buffer for the data items
	 @return count of the data items were read successfully.
	 @throws java.io.EOFException if no data item available more
	 @throws java.io.IOException if failed to read some-why
	 */
	int read(final List<T> buffer)
	throws IOException;

	/**
	 Reset this input making this readable from the beginning
	 */
	void reset()
	throws IOException;
}
