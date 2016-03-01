package com.emc.mongoose.common.generator;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.FastDateFormat;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static com.emc.mongoose.common.generator.AsyncDateGenerator.*;
import static org.apache.commons.lang.time.DateUtils.*;

public class AsyncStringDateGenerator
extends AsyncRangeGeneratorBase<String> {

	private final static FastDateFormat outputDateFormat =
			FastDateFormat.getInstance(OUTPUT_DATE_FMT_STRINGS[0]);

	private final AsyncDateGenerator dateGenerator;

	public AsyncStringDateGenerator(final String minValue, final String maxValue)
	throws ParseException {
		super(minValue, maxValue);
		dateGenerator = new AsyncDateGenerator(
				parseDate(minValue, INPUT_DATE_FMT_STRINGS),
				parseDate(maxValue, INPUT_DATE_FMT_STRINGS));
	}

	public AsyncStringDateGenerator(String initialValue)
	throws ParseException {
		super(initialValue);
		String[] zones = TimeZone.getAvailableIDs();
		dateGenerator = new AsyncDateGenerator(DateUtils.parseDate(initialValue, INPUT_DATE_FMT_STRINGS));
	}

	@Override
	protected final String computeRange(String minValue, String maxValue) {
		return null;
	}

	@Override
	protected final String rangeValue() {
		return outputDateFormat.format(dateGenerator.get());
	}

	@Override
	protected final String singleValue() {
		return outputDateFormat.format(dateGenerator.get());
	}

	@Override
	public final boolean isInitialized() {
		return dateGenerator != null;
	}
}
