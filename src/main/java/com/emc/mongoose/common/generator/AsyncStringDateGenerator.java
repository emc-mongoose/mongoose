package com.emc.mongoose.common.generator;

import java.text.ParseException;

import static com.emc.mongoose.common.generator.AsyncDateGenerator.FMT_DATE;

public class AsyncStringDateGenerator
extends AsyncRangeGeneratorBase<String> {

	private final AsyncDateGenerator dateGenerator;

	public AsyncStringDateGenerator(final String minValue, final String maxValue)
	throws ParseException {
		super(minValue, maxValue);
		dateGenerator = new AsyncDateGenerator(FMT_DATE.parse(minValue), FMT_DATE.parse(maxValue));
	}

	public AsyncStringDateGenerator(String initialValue)
	throws ParseException {
		super(initialValue);
		dateGenerator = new AsyncDateGenerator(FMT_DATE.parse(initialValue));
	}

	@Override
	protected final String computeRange(String minValue, String maxValue) {
		return null;
	}

	@Override
	protected final String rangeValue() {
		return FMT_DATE.format(dateGenerator.get());
	}

	@Override
	protected final String singleValue() {
		return FMT_DATE.format(dateGenerator.get());
	}

	@Override
	public final boolean isInitialized() {
		return dateGenerator != null;
	}
}
