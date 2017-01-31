package com.emc.mongoose.tests.unit.util;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsNull;

import static org.hamcrest.core.IsNot.not;

/**
 Created on 13.07.16.
 */
public class ConfigNullMatcher<T> extends IsNull<T> {

	private final String path;

	public ConfigNullMatcher(final String path) {
		this.path = path;
	}

	@Factory
	public static Matcher<Object> nullValue(final String path) {
		return new ConfigNullMatcher<>(path);
	}

	@Factory
	public static Matcher<Object> notNullValue() {
		return not(nullValue());
	}

	@Factory
	public static <T> Matcher<T> nullValue(Class<T> type, final String path) {
		return new ConfigNullMatcher<>(path);
	}

	@Factory
	public static <T> Matcher<T> notNullValue(Class<T> type) {
		return not(nullValue(type));
	}

	@Override
	public void describeMismatch(final Object item, final Description description) {
		description.appendText("parameter was - ").appendValue(item);
	}

	@Override
	public void describeTo(final Description description) {
		description.appendText("parameter '").appendText(path).appendText("' was - ").appendValue("null");
	}
}
