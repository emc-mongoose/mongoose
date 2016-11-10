package com.emc.mongoose.common.io;

import com.emc.mongoose.common.exception.UserShootHisFootException;

public interface ValueInputFactory<T, G extends Input<T>> {

	Enum defineState(final String... parameters);

	G createInput(final char type, final String... parameters)
	throws UserShootHisFootException;

}
