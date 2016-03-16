package com.emc.mongoose.common.generator;

public interface GeneratorFactory<T, G extends ValueGenerator<T>> {

	Enum defineState(final String ... parameters);
	G createGenerator(final char type, final String ... parameters);

}
