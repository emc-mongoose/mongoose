package com.emc.mongoose.core.api.item.data;
//
import com.emc.mongoose.core.api.item.base.FileItemInput;
/**
 Created by kurila on 11.08.15.
 */
public interface FileDataItemInput<T extends DataItem>
extends FileItemInput<T> {
	long getAvgDataSize(final int maxCount);
}
