package com.emc.mongoose.common.generator;

import org.apache.commons.lang.time.DateUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
public final class AsyncDateGenerator
extends AsyncRangeGeneratorBase<Date> {

	public final static String[] INPUT_DATE_FMT_STRINGS = new String[]{"yyyy/MM/dd"};
	public final static String[] OUTPUT_DATE_FMT_STRINGS = new String[]{"yyyy-MM-dd'T'HH:mm:ssZ"};

	private final AsyncLongGenerator longGenerator;

	public AsyncDateGenerator(final Date minValue, final Date maxValue) {
		super(minValue, maxValue);
		longGenerator = new AsyncLongGenerator(minValue.getTime(), maxValue.getTime());
	}

	public AsyncDateGenerator(final Date initialValue) throws ParseException{
		super(initialValue);
		longGenerator = new AsyncLongGenerator(
				DateUtils.parseDate("1970/01/01", INPUT_DATE_FMT_STRINGS).getTime(), initialValue.getTime()
		);
	}

	@Override
	protected final Date computeRange(final Date minValue, final Date maxValue) {
		return null;
	}

	@Override
	protected final Date rangeValue() {
		return new Date(longGenerator.get());
	}

	@Override
	protected final Date singleValue() {
		return new Date(longGenerator.get());
	}

	@Override
	public final boolean isInitialized() {
		return longGenerator != null;
	}
}
