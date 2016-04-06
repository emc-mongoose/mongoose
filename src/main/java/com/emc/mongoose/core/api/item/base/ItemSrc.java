package com.emc.mongoose.core.api.item.base;
//
//
import com.emc.mongoose.common.io.Input;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;

/**
 Created by kurila on 17.06.15.
 */
public interface ItemSrc<T extends Item>
extends Input<T> {

	String
		MSG_SKIP_START = "Skipping {} items. This may take some time to complete. Please wait...",
		MSG_SKIP_END = "Items have been skipped";
	/**
	 * Set last processed item.
	 * @param lastItem last processed item
	 */
	void setLastItem(final T lastItem);

}
