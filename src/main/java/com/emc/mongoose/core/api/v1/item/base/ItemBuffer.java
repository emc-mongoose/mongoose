package com.emc.mongoose.core.api.v1.item.base;
//
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
/**
 Created by kurila on 30.09.15.
 */
public interface ItemBuffer<T extends Item>
extends Input<T>, Output<T> {
	//
	boolean isEmpty();
	//
	int size();
}
