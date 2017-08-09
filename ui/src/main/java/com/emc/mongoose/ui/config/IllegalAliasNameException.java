package com.emc.mongoose.ui.config;

/**
 Created by andrey on 27.02.17.
 */
public class IllegalAliasNameException
extends IllegalArgumentException {

	public IllegalAliasNameException(final String name) {
		super(name);
	}
}