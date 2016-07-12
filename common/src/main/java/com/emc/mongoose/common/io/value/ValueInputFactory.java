package com.emc.mongoose.common.io.value;

import com.emc.mongoose.common.io.Input;

public interface ValueInputFactory<T, G extends Input<T>> {

	Enum defineState(final String... parameters);
	G createInput(final char type, final String... parameters);

}
