package com.emc.mongoose.common.generator;
/**
 Created by kurila on 10.02.16.
 */
public interface ValueGenerator<T> {
	String DELIMITER = ";";
	T get();
}
