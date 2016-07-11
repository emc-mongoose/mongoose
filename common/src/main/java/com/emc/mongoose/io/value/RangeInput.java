package com.emc.mongoose.io.value;

import com.emc.mongoose.io.Input;

/**
 Created by kurila on 11.03.16.
 */
public interface RangeInput<T>
extends Input<String> {
	T value();
}
