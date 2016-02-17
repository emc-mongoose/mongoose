package com.emc.mongoose.common.generator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.ParseException;

import static com.emc.mongoose.common.generator.AsyncDateGenerator.FMT_DATE;

public class AsyncStringDateGenerator
extends AsyncRangeGeneratorBase<String> {

	private final static Logger LOG = LogManager.getLogger();

	private final AsyncDateGenerator dateGenerator;

	public AsyncStringDateGenerator(String minValue, String maxValue)
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
	protected String computeRange(String minValue, String maxValue) {
		return null;
	}

	@Override
	protected String rangeValue() {
		return FMT_DATE.format(dateGenerator.get());
	}

	@Override
	protected String singleValue() {
		return FMT_DATE.format(dateGenerator.get());
	}

	@Override
	public boolean isInitialized() {
		return dateGenerator != null;
	}
}
