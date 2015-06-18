package com.emc.mongoose.util.client.api;
//
import com.emc.mongoose.core.api.data.DataItem;

import java.io.Closeable;
import java.io.IOException;
/**
 Created by kurila on 17.06.15.
 */
public interface DataItemInput<T extends DataItem>
extends Closeable {

	/**
	 Get next data item
	 @return next data item or null if no data item available
	 @throws java.io.IOException if failed to read some-why
	 */
	T read()
	throws IOException;

	/**
	 Reset this input making this readable from the beginning
	 */
	void reset()
	throws IOException;
}
