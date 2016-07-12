package com.emc.mongoose.common.item;

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
