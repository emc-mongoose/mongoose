package com.emc.mongoose.core.api.item.data;
//
import com.emc.mongoose.core.api.item.base.ItemFileSrc;
/**
 Created by kurila on 11.08.15.
 */
public interface DataItemFileSrc<T extends DataItem>
extends ItemFileSrc<T> {
	long getAvgDataSize(final int maxCount);
}
