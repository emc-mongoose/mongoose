package com.emc.mongoose.common.generator;

import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.emc.mongoose.common.generator.AsyncDateGenerator.FMT_DATE;

public final class AsyncRangeGeneratorFactory {

	// pay attention to the matcher groups
	public static final String DOUBLE_REG_EXP = "([-+]?\\d*\\.?\\d+)";
	public static final String LONG_REG_EXP = "([-+]?\\d+)";
	public static final String DATE_REG_EXP = "(((19|20)[0-9][0-9])/(0?[1-9]|1[012])/(0?[1-9]|[12][0-9]|3[01]))";
	private static final Pattern DOUBLE_PATTERN = Pattern.compile(rangeRegExp(DOUBLE_REG_EXP));
	private static final Pattern LONG_PATTERN = Pattern.compile(rangeRegExp(LONG_REG_EXP));
	private static final Pattern DATE_PATTERN = Pattern.compile(rangeRegExp(DATE_REG_EXP));

	private AsyncRangeGeneratorFactory() {
	}

	// Pay attention to the escape symbols
	private static String rangeRegExp(final String typeRegExp) {
		return typeRegExp + AsyncFormattingGenerator.RANGE_DELIMITER + typeRegExp;
	}

	public static ValueGenerator createGenerator(final char type)
	throws ParseException {
		switch (type) {
			case 'f':
				return new AsyncDoubleGenerator(47.0);
			case 'd':
				return new AsyncLongGenerator(47L);
			case 'D':
				return new AsyncStringDateGenerator(FMT_DATE.format(new Date()));
			default:
				throw new IllegalArgumentException();
		}
	}

	public static ValueGenerator createGenerator(final char type, final String range)
	throws IllegalArgumentException, ParseException {
		final Matcher matcher;
		switch (type) {
			case 'f':
				matcher = DOUBLE_PATTERN.matcher(range);
				if (matcher.find()) {
					return new AsyncDoubleGenerator(Double.valueOf(matcher.group(1)), Double.valueOf(matcher.group(2)));
				} else {
					throw new IllegalArgumentException();
				}
			case 'd':
				matcher = LONG_PATTERN.matcher(range);
				if (matcher.find()) {
					return new AsyncLongGenerator(Long.valueOf(matcher.group(1)), Long.valueOf(matcher.group(2)));
				} else {
					throw new IllegalArgumentException();
				}
			case 'D':
				matcher = DATE_PATTERN.matcher(range);
				if (matcher.find()) {
					return new AsyncStringDateGenerator(matcher.group(1), matcher.group(6));
				} else {
					throw new IllegalArgumentException();
				}
			default:
				throw new IllegalArgumentException();
		}
	}
}
