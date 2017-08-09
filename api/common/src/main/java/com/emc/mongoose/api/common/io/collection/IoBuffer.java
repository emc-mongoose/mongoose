package com.emc.mongoose.api.common.io.collection;

import com.emc.mongoose.api.common.io.Input;
import com.emc.mongoose.api.common.io.Output;

/**
 Created by kurila on 30.09.15.
 */
public interface IoBuffer<T>
extends Input<T>, Output<T> {
	
	boolean isEmpty();
	
	int size();
}
