package com.emc.mongoose.ui.config;

/**
 Created by andrey on 27.02.17.
 */
public class IllegalAliasTargetException
extends IllegalArgumentException {

	public IllegalAliasTargetException(final String name) {
		super(name);
	}
}
