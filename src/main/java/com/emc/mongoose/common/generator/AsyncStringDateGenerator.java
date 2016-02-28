package com.emc.mongoose.common.generator;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static com.emc.mongoose.common.generator.AsyncDateGenerator.*;

public class AsyncStringDateGenerator
extends AsyncRangeGeneratorBase<String> {

	private final AsyncDateGenerator dateGenerator;
	private final DateFormat dateFormat = new SimpleDateFormat(DATE_FMT_STRING);

	public AsyncStringDateGenerator(final String minValue, final String maxValue)
	throws ParseException {
		super(minValue, maxValue);
		dateGenerator = new AsyncDateGenerator(dateFormat.parse(minValue), dateFormat.parse(maxValue));
	}

	public AsyncStringDateGenerator(String initialValue)
	throws ParseException {
		super(initialValue);
		dateGenerator = new AsyncDateGenerator(dateFormat.parse(initialValue));
	}

	@Override
	protected final String computeRange(String minValue, String maxValue) {
		return null;
	}

	@Override
	protected final String rangeValue() {
		return dateFormat.format(dateGenerator.get());
	}

	@Override
	protected final String singleValue() {
		return dateFormat.format(dateGenerator.get());
	}

	@Override
	public final boolean isInitialized() {
		return dateGenerator != null;
	}
}
