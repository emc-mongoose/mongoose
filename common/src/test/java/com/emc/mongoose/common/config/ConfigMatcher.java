package com.emc.mongoose.common.config;

import org.hamcrest.Description;
import org.hamcrest.core.IsEqual;

/**
 Created on 13.07.16.
 */
public class ConfigMatcher<T> extends IsEqual<T> {

	private final String path;

	public ConfigMatcher(final T equalArg, final String path) {
		super(equalArg);
		this.path = path;
	}

	@Override
	public void describeMismatch(final Object item, final Description description) {
//		description.appendText("")
	}
}
