package com.emc.mongoose.common.io.range;

import com.emc.mongoose.common.io.Input;
/**
 Created by kurila on 11.03.16.
 */
public interface RangeInput<T>
extends Input<String> {

	T value();
}
