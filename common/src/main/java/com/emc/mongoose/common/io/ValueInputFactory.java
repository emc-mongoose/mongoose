package com.emc.mongoose.common.io;

import com.emc.mongoose.common.exception.UserShootHisFootException;

public interface ValueInputFactory<T, G extends Input<T>> {

	// pay attention to the matcher groups
	String DOUBLE_REG_EXP = "([-+]?\\d*\\.?\\d+)";
	String LONG_REG_EXP = "([-+]?\\d+)";
	String DATE_REG_EXP = "(((19|20)[0-9][0-9])/(1[012]|0?[1-9])/(3[01]|[12][0-9]|0?[1-9]))";

	Enum defineState(final String... parameters);

	G createInput(final char type, final String... parameters)
	throws UserShootHisFootException;

}
