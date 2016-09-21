package com.emc.mongoose.ui.config.matcher;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 Created on 13.07.16.
 */
public class ConfigMatcher<T> extends TypeSafeMatcher<T> {

	private final T expectedValue;
	private final String path;

	public ConfigMatcher(final T equalArg, final String path) {
		this.expectedValue = equalArg;
		this.path = path;
	}

	private static <T> boolean areEqual(T actual, T expected) {
		if(actual == null) {
			return expected == null;
		}
		return actual.equals(expected);
	}

	@Override
	public boolean matchesSafely(final T actualValue) {
		return areEqual(actualValue, expectedValue);
	}

	@Override
	public void describeMismatchSafely(final T item, final Description description) {
		description.appendText("parameter was - ").appendValue(item);
	}

	@Factory
	public static <T> Matcher<T> equalTo(final T operand, final String path) {
		return new ConfigMatcher<>(operand, path);
	}

	@Override
	public void describeTo(final Description description) {
		description.appendText("parameter '").appendText(path).appendText("' should be - ").appendValue(expectedValue);
	}
}
