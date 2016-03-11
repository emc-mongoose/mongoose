package com.emc.mongoose.common.generator.async;
import com.emc.mongoose.common.generator.CompositeFormattingGenerator;
import com.emc.mongoose.common.generator.FormattingGenerator;
public final class AsyncFormattingGenerator
extends AsyncValueGenerator<String>
implements FormattingGenerator {

	private final FormattingGenerator wrappedGenerator;

	public AsyncFormattingGenerator(final String pattern) {
		this(
			new CompositeFormattingGenerator(
				pattern, AsyncStringGeneratorFactory.getInstance()
			)
		);
	}

	private AsyncFormattingGenerator(final FormattingGenerator wrappedGenerator) {
		super(
			null,
			new InitializedCallableBase<String>() {
				private final StringBuilder result = new StringBuilder();
				@Override
				public String call()
				throws Exception {
					result.setLength(0);
					return wrappedGenerator.format(result);
				}
			}
		);
		this.wrappedGenerator = wrappedGenerator;
	}

	@Override
	public String getPattern() {
		return wrappedGenerator.getPattern();
	}

	@Override
	public String format(final StringBuilder result) {
		return wrappedGenerator.format(result);
	}
}
