package com.emc.mongoose.common.generator.async;
import com.emc.mongoose.common.generator.FormatRangeGenerator;
import com.emc.mongoose.common.generator.FormatGenerator;
public final class AsyncFormatGenerator
extends AsyncValueGenerator<String>
implements FormatGenerator {

	private final FormatGenerator wrappedGenerator;

	public AsyncFormatGenerator(final String pattern) {
		this(
			new FormatRangeGenerator(
				pattern, AsyncStringGeneratorFactory.getInstance()
			)
		);
	}

	private AsyncFormatGenerator(final FormatGenerator wrappedGenerator) {
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
