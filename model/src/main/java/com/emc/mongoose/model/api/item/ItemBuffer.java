package com.emc.mongoose.model.api.item;

import com.emc.mongoose.model.api.io.Input;
import com.emc.mongoose.model.api.io.Output;

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
