package com.emc.mongoose.common.generator;

import java.text.ParseException;

public class FormattingGenerator
extends FormattingGeneratorSkeleton
implements ValueGenerator<String> {

	public FormattingGenerator(String pattern)
	throws ParseException {
		super(pattern);
		initialize(pattern, countPatternSymbols(pattern));
	}

	@Override
	public String get() {
		return format(new StringBuilder());
	}
}
