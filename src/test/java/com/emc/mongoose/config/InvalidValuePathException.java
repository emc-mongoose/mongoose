package com.emc.mongoose.config;

public class InvalidValuePathException
extends IllegalArgumentException {

	public InvalidValuePathException(final String path) {
		super(path);
	}
}
