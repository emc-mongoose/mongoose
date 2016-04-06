package com.emc.mongoose.common.io.value.async;
//
import com.emc.mongoose.common.io.value.RangePatternDefinedInput;
import com.emc.mongoose.common.io.value.PatternDefinedInput;
//
public final class AsyncPatternDefinedInput
extends AsyncValueInput<String>
implements PatternDefinedInput {
	//
	private final PatternDefinedInput wrappedGenerator;
	//
	public
	AsyncPatternDefinedInput(final String pattern) {
		this(
			new RangePatternDefinedInput(
				pattern, AsyncStringInputFactory.getInstance()
			)
		);
	}
	//
	private
	AsyncPatternDefinedInput(final PatternDefinedInput wrappedGenerator) {
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
	//
	@Override
	public String getPattern() {
		return wrappedGenerator.getPattern();
	}

	@Override
	public String format(final StringBuilder result) {
		return wrappedGenerator.format(result);
	}
}
