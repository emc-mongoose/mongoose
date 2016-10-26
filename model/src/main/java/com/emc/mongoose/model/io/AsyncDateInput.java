package com.emc.mongoose.model.io;

import com.emc.mongoose.common.exception.OmgDoesNotPerformException;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.FastDateFormat;

import java.text.Format;
import java.text.ParseException;
import java.util.Date;

public final class AsyncDateInput
extends AsyncFormatRangeInputBase<Date> {

	public static final String[] INPUT_DATE_FMT_STRINGS = new String[] { "yyyy/MM/dd" };

	private final RangeInput<Long> longGenerator;

	public AsyncDateInput(final Date minValue, final Date maxValue, final String formatString)
	throws OmgDoesNotPerformException{
		super(minValue, maxValue, formatString);
		longGenerator = new AsyncLongInput(minValue.getTime(), maxValue.getTime());
	}

	public AsyncDateInput(final Date initialValue, final String formatString)
	throws OmgDoesNotPerformException {
		super(initialValue, formatString);
		try {
			longGenerator = new AsyncLongInput(
				DateUtils.parseDate("1970/01/01", INPUT_DATE_FMT_STRINGS).getTime(),
				initialValue.getTime()
			);
		} catch(final ParseException e) {
			throw new OmgDoesNotPerformException(e);
		}
	}

	/**
	 *
	 * @param formatString - a pattern for SimpleDateFormat. It should match a date pattern in ISO 8601 format.
	 *                        For details see
	 *                        https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
	 * @return a suitable formatter for dates
	 */
	@Override
	protected final Format getFormatterInstance(final String formatString) {
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
	protected final String stringify(final Date value) {
		return outputFormat().format(value);
	}

	@Override
	public final boolean isInitialized() {
		return longGenerator != null;
	}
}
