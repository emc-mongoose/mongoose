package com.emc.mongoose.common.generator;

import java.text.Format;

public abstract class AsyncFormatRangeGeneratorBase<T> extends AsyncRangeGeneratorBase<T> {

	protected final Format outputFormat;

	protected AsyncFormatRangeGeneratorBase(T minValue, T maxValue, String formatString) {
		super(minValue, maxValue);
		outputFormat = getFormatterInstance(formatString);
	}

	public AsyncFormatRangeGeneratorBase(T initialValue, String formatString) {
		super(initialValue);
		outputFormat = getFormatterInstance(formatString);
	}

	abstract Format getFormatterInstance(String formatString);
}
