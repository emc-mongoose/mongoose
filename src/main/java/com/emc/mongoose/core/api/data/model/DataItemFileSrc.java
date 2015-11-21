package com.emc.mongoose.core.api.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;
/**
 Created by kurila on 11.08.15.
 */
public interface DataItemFileSrc<T extends DataItem>
extends ItemFileSrc<T> {
	long getApproxDataItemsSize(final int maxCount);
}
