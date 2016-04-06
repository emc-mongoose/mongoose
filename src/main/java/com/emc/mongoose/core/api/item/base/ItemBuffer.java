package com.emc.mongoose.core.api.item.base;
//
/**
 Created by kurila on 30.09.15.
 */
public interface ItemBuffer<T extends Item>
extends Output<T>, ItemSrc<T> {
	//
	boolean isEmpty();
	//
	int size();
}
