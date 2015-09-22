package com.emc.mongoose.core.api.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
/**
 Created by kurila on 17.06.15.
 */
public interface DataItemDst<T extends DataItem>
extends Closeable {

	/**
	 Write the data item
	 @param dataItem the data item to put
	 @throws IOException if fails some-why
	 */
	void put(final T dataItem)
	throws IOException, InterruptedException, RejectedExecutionException;

	/**
	 Bulk put method for the data items from the specified buffer
	 @param buffer the buffer containing the data items to put
	 @return the count of the data items successfully written
	 @throws IOException
	 */
	int put(final List<T> buffer, final int from, final int to)
	throws IOException, InterruptedException, RejectedExecutionException;

	/**
	 Make a {@link DataItemSrc} instance from this.
	 @return {@link DataItemSrc} instance containing the items which had been written to this output.
	 @throws IOException
	 */
	DataItemSrc<T> getDataItemSrc()
	throws IOException;
}
