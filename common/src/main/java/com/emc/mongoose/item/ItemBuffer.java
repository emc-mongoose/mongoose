package com.emc.mongoose.item;

import com.emc.mongoose.io.Input;
import com.emc.mongoose.io.Output;

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
