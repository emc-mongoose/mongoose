package com.emc.mongoose.core.api.item.base;
//
//
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
/**
 Created by kurila on 17.06.15.
 */
public interface ItemDst<T extends Item>
extends Closeable {

	/**
	 Write the data item
	 @param dataItem the item to put
	 @throws IOException if fails some-why
	 */
	void put(final T dataItem)
	throws IOException;

	/**
	 Bulk put method for the items from the specified buffer
	 @param buffer the buffer containing the items to put
	 @return the count of the items successfully written
	 @throws IOException
	 */
	int put(final List<T> buffer, final int from, final int to)
	throws IOException;

	int put(final List<T> buffer)
	throws IOException;

	/**
	 Make a {@link ItemSrc} instance from this.
	 @return {@link ItemSrc} instance containing the items which had been written to this output.
	 @throws IOException
	 */
	ItemSrc<T> getItemSrc()
	throws IOException;
}
