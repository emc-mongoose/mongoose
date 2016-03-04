package com.emc.mongoose.common.generator;

public interface GeneratorFactory<T> {

	Enum defineState(String ... parameters);
	ValueGenerator<T> createGenerator(
			final char type, final String ... parameters) throws Exception;

}
