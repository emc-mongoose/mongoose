package com.emc.mongoose.model.api.item;

import com.emc.mongoose.model.api.io.Input;
import com.emc.mongoose.model.api.io.Output;

/**
 Created by kurila on 30.09.15.
 */
public interface ItemBuffer<I extends Item>
extends Input<I>, Output<I> {
	//
	boolean isEmpty();
	//
	int size();
}
