package com.emc.mongoose.model.api.io;

import com.emc.mongoose.common.exception.UserShootItsFootException;

public interface ValueInputFactory<T, G extends Input<T>> {

	Enum defineState(final String... parameters);

	G createInput(final char type, final String... parameters)
	throws UserShootItsFootException;

}
