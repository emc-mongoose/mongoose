package com.emc.mongoose.model.api.io;

/**
 Created by kurila on 11.03.16.
 */
public interface RangeInput<T>
extends Input<String> {
	T value();
}
