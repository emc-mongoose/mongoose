package com.emc.mongoose.common.generator;

import java.text.ParseException;

import static com.emc.mongoose.common.generator.FormattingGeneratorSkeleton.countPatternSymbols;

public final class AsyncFormattingGenerator
extends AsyncValueGenerator<String>
implements ValueGenerator<String> {

	public AsyncFormattingGenerator(final String pattern)
	throws ParseException {
		this(pattern, new FormattingGeneratorSkeleton(pattern));
	}

	private AsyncFormattingGenerator(
		final String pattern, final FormattingGeneratorSkeleton innerGenerator) throws ParseException {
		super(
			null,
			new InitCallable<String>() {
				private final StringBuilder result = new StringBuilder();
				@Override
				public String call()
				throws Exception {
					return innerGenerator.format(result);
				}
				@Override
				public boolean isInitialized() {
					int lastIndex = innerGenerator.segments().length - 1;
					return innerGenerator.segments()[lastIndex] != null;
				}
			}
		);
		innerGenerator.initialize(pattern, countPatternSymbols(pattern));
	}

}
