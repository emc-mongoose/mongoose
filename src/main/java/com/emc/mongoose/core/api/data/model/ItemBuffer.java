package com.emc.mongoose.core.api.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;
/**
 Created by kurila on 30.09.15.
 */
public interface ItemBuffer<T extends DataItem>
extends DataItemDst<T>, DataItemSrc<T> {
	boolean isEmpty();
}
