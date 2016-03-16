package com.emc.mongoose.common.generator;
/**
 Created by kurila on 11.03.16.
 */
public interface RangeGenerator<T>
extends ValueGenerator<String> {
	T value();
}
