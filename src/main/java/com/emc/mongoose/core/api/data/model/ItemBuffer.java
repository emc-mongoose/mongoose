package com.emc.mongoose.core.api.data.model;
//
import com.emc.mongoose.core.api.Item;
/**
 Created by kurila on 30.09.15.
 */
public interface ItemBuffer<T extends Item>
extends ItemDst<T>, ItemSrc<T> {
	boolean isEmpty();
}
