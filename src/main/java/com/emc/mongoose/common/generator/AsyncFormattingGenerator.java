package com.emc.mongoose.common.generator;

import java.util.concurrent.Callable;

public final class AsyncFormattingGenerator
extends AsyncValueGenerator<String>
implements ValueGenerator<String> {

	public AsyncFormattingGenerator(final String pattern) {
		this(new FormattingGenerator(pattern, AsyncStringGeneratorFactory.generatorFactory()));
	}

	private AsyncFormattingGenerator(final FormattingGenerator innerGenerator) {
		super(
			null,
			new Callable<String>() {
				private final StringBuilder result = new StringBuilder();
				@Override
				public String call()
				throws Exception {
					result.setLength(0);
					return innerGenerator.format(result);
				}
			}
		);
	}

}
