package com.emc.mongoose.core.api.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;
/**
 Created by kurila on 11.08.15.
 */
public interface FileDataItemSrc<T extends DataItem>
extends FileItemSrc<T> {
	long getApproxDataItemsSize(final int maxCount);
}
