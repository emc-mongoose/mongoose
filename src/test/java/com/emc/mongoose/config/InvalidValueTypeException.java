package com.emc.mongoose.config;

public final class InvalidValueTypeException
extends IllegalStateException {

	private final String path;

	public InvalidValueTypeException(
		final String path, final Class expectedType, final Class actualType
	) {
		super(
			"Invalid value type @ " + path + ", expected: \"" + expectedType + "\", actual: \""
				+ actualType + "\""
		);
		this.path = path;
	}

	public final String path() {
		return path;
	}
}
