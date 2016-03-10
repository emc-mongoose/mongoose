package com.emc.mongoose.common.generator;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.FastDateFormat;

import java.text.Format;
import java.text.ParseException;
import java.util.Date;

public final class AsyncDateGenerator
extends AsyncFormatRangeGeneratorBase<Date> {

	public final static String[] INPUT_DATE_FMT_STRINGS = new String[]{"yyyy/MM/dd"};

	private final AsyncLongGenerator longGenerator;

	public AsyncDateGenerator(final Date minValue, final Date maxValue, final String formatString) {
		super(minValue, maxValue, formatString);
		longGenerator = new AsyncLongGenerator(minValue.getTime(), maxValue.getTime());
	}

	public AsyncDateGenerator(final Date initialValue, final String formatString) throws ParseException{
		super(initialValue, formatString);
		longGenerator = new AsyncLongGenerator(
				DateUtils.parseDate("1970/01/01", INPUT_DATE_FMT_STRINGS).getTime(), initialValue.getTime()
		);
	}

	/**
	 *
	 * @param formatString - a pattern for SimpleDateFormat. It should match a date pattern in ISO 8601 format.
	 *                        For details see
	 *                        https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
	 * @return a suitable formatter for dates
	 */
	@Override
	Format getFormatterInstance(String formatString) {
		return FastDateFormat.getInstance(formatString);
	}

	@Override
	protected final Date computeRange(final Date minValue, final Date maxValue) {
		return null;
	}

	@Override
	protected final Date rangeValue() {
		return new Date(longGenerator.value());
	}

	@Override
	protected final Date singleValue() {
		return new Date(longGenerator.value());
	}

	@Override
	protected String stringify(Date value) {
		return outputFormat().format(value);
	}

	@Override
	public final boolean isInitialized() {
		return longGenerator != null;
	}
}
