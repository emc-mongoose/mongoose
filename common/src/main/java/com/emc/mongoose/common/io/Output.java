package com.emc.mongoose.common.io;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 Created by andrey on 06.04.16.
 */
public interface Output<I>
extends Closeable {

	/**
	 Write the data item
	 @param item the item to put
	 @throws IOException if fails some-why
	 */
	boolean put(final I item)
	throws IOException;

	/**
	 Bulk put method for the items from the specified buffer
	 @param buffer the buffer containing the items to put
	 @return the count of the items successfully written
	 @throws IOException
	 */
	int put(final List<I> buffer, final int from, final int to)
	throws IOException;

	int put(final List<I> buffer)
	throws IOException;

	/**
	 Make a {@link Input} instance from this.
	 @return {@link Input} instance containing the items which had been written to this output.
	 @throws IOException
	 */
	Input<I> getInput()
	throws IOException;
}
