package com.emc.mongoose.io.value;

import com.emc.mongoose.io.Input;

public interface ValueInputFactory<T, G extends Input<T>> {

	Enum defineState(final String... parameters);
	G createInput(final char type, final String... parameters);

}
