package com.emc.mongoose.core.api.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
/**
 Created by kurila on 17.06.15.
 */
public interface DataItemOutput<T extends DataItem>
extends Closeable {

	/**
	 Write the data item
	 @param dataItem the data item to write
	 @throws IOException if fails some-why
	 */
	void write(final T dataItem)
	throws IOException;

	/**
	 Bulk write method for the data items from the specified buffer
	 @param buffer the buffer containing the data items to write
	 @return the count of the data items successfully written
	 @throws IOException
	 */
	int write(final List<T> buffer)
	throws IOException;

	/**
	 Make a {@link DataItemInput} instance from this.
	 @return {@link DataItemInput} instance containing the items which had been written to this output.
	 @throws IOException
	 */
	DataItemInput<T> getInput()
	throws IOException;
}
