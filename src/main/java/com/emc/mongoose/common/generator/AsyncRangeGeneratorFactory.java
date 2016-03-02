package com.emc.mongoose.common.generator;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.emc.mongoose.common.generator.AsyncDateGenerator.*;
import static org.apache.commons.lang.time.DateUtils.*;

public final class AsyncRangeGeneratorFactory {

	// pay attention to the matcher groups
	public static final String DOUBLE_REG_EXP = "([-+]?\\d*\\.?\\d+)";
	public static final String LONG_REG_EXP = "([-+]?\\d+)";
	public static final String DATE_REG_EXP = "(((19|20)[0-9][0-9])/(1[012]|0?[1-9])/(3[01]|[12][0-9]|0?[1-9]))";
	private static final Pattern DOUBLE_PATTERN = Pattern.compile(rangeRegExp(DOUBLE_REG_EXP));
	private static final Pattern LONG_PATTERN = Pattern.compile(rangeRegExp(LONG_REG_EXP));
	private static final Pattern DATE_PATTERN = Pattern.compile(rangeRegExp(DATE_REG_EXP));

	private AsyncRangeGeneratorFactory() {
	}

	// Pay attention to the escape symbols
	private static String rangeRegExp(final String typeRegExp) {
		return typeRegExp + AsyncFormattingGenerator.RANGE_DELIMITER + typeRegExp;
	}

	private enum States {
		EMPTY, RANGE, FORMAT, FORMAT_RANGE
	}

	private static States defineState(String format, String range) {
		if (format == null) {
			if (range == null) {
				return States.EMPTY;
			} else {
				return States.RANGE;
			}
		} else {
			if (range == null) {
				return States.FORMAT;
			} else {
				return States.FORMAT_RANGE;
			}
		}
	}

	/**
	 *
	 * @param type - a type of the generator
	 * @param parameters - an array of parameters (if length > 1, first arg - a format, second - a range, by default)
	 * @return a suitable generator
	 * @throws ParseException
	 */
	public static ValueGenerator<String> createGenerator(final char type, final String ... parameters)
			throws ParseException {
		States state = defineState(parameters[0], parameters[1]);
		final Matcher matcher;
		switch (state) {
			case EMPTY:
				switch (type) {
					case 'd':
						return new AsyncLongGenerator(47L);
					default:
						throw new IllegalArgumentException();
				}
			case FORMAT:
				switch (type) {
					case 'f':
						return new AsyncDoubleGenerator(47.0, parameters[0]);
					case 'D':
						return new AsyncDateGenerator(parseDate("2016/02/25", INPUT_DATE_FMT_STRINGS), parameters[0]);
					default:
						throw new IllegalArgumentException();
				}
			case RANGE:
				switch (type) {
					case 'd':
						matcher = LONG_PATTERN.matcher(parameters[1]);
						if (matcher.find()) {
							return new AsyncLongGenerator(
									Long.valueOf(matcher.group(1)),
									Long.valueOf(matcher.group(2)));
						} else {
							throw new IllegalArgumentException();
						}
				}
			case FORMAT_RANGE:
				switch (type) {
					case 'f':
						matcher = DOUBLE_PATTERN.matcher(parameters[1]);
						if (matcher.find()) {
							return new AsyncDoubleGenerator(
									Double.valueOf(matcher.group(1)),
									Double.valueOf(matcher.group(2)),
									parameters[0]);
						} else {
							throw new IllegalArgumentException();
						}
					case 'D':
						matcher = DATE_PATTERN.matcher(parameters[1]);
						if (matcher.find()) {
							return new AsyncDateGenerator(
									parseDate(matcher.group(1), INPUT_DATE_FMT_STRINGS),
									parseDate(matcher.group(6), INPUT_DATE_FMT_STRINGS),
									parameters[0]);
						} else {
							throw new IllegalArgumentException();
						}
					default:
						throw new IllegalArgumentException();
				}
			default:
				throw new IllegalArgumentException();
		}
	}

}
