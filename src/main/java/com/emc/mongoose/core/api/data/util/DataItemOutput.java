package com.emc.mongoose.core.api.data.util;
//
import com.emc.mongoose.core.api.data.DataItem;

import java.io.Closeable;
import java.io.IOException;
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
	 Make a {@link DataItemInput} instance from this.
	 @return {@link DataItemInput} instance containing the items which had been written to this output.
	 @throws IOException
	 */
	DataItemInput<T> getInput()
	throws IOException;
}
