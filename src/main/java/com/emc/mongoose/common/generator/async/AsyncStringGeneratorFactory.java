package com.emc.mongoose.common.generator.async;

import com.emc.mongoose.common.generator.GeneratorFactory;
import com.emc.mongoose.common.generator.ValueGenerator;
import com.emc.mongoose.common.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.emc.mongoose.common.generator.async.AsyncDateGenerator.*;
import static com.emc.mongoose.common.generator.CompositeFormattingGenerator.RANGE_DELIMITER;
import static org.apache.commons.lang.time.DateUtils.*;

public final class AsyncStringGeneratorFactory<G extends ValueGenerator<String>>
implements GeneratorFactory<String, G> {

	private static final Logger LOG = LogManager.getLogger();
	// pay attention to the matcher groups
	public static final String DOUBLE_REG_EXP = "([-+]?\\d*\\.?\\d+)";
	public static final String LONG_REG_EXP = "([-+]?\\d+)";
	public static final String DATE_REG_EXP = "(((19|20)[0-9][0-9])/(1[012]|0?[1-9])/(3[01]|[12][0-9]|0?[1-9]))";
	private static final Pattern DOUBLE_PATTERN = Pattern.compile(rangeRegExp(DOUBLE_REG_EXP));
	private static final Pattern LONG_PATTERN = Pattern.compile(rangeRegExp(LONG_REG_EXP));
	private static final Pattern DATE_PATTERN = Pattern.compile(rangeRegExp(DATE_REG_EXP));

	private final static AsyncStringGeneratorFactory INSTANCE = new AsyncStringGeneratorFactory();

	private AsyncStringGeneratorFactory() {
	}

	public static AsyncStringGeneratorFactory getInstance() {
		return INSTANCE;
	}

	// Pay attention to the escape symbols
	private static String rangeRegExp(final String typeRegExp) {
		return typeRegExp + RANGE_DELIMITER + typeRegExp;
	}

	private enum State {
		EMPTY, RANGE, FORMAT, FORMAT_RANGE
	}

	/**
	 * This enum is used to ease understanding of switch-case in createGenerator() method.
	 * @param parameters :
	 * [0] an output format for an AsyncFormatRangeGeneratorBase generator
	 * [1] a range of random generator AsyncRangeGeneratorBase values
	 * @return a state that defines a choice of the generator by the factory
	 */
	@Override
	public Enum defineState(final String... parameters) {
		if (parameters[0] == null) {
			if (parameters[1] == null) {
				return State.EMPTY;
			} else {
				return State.RANGE;
			}
		} else {
			if (parameters[1] == null) {
				return State.FORMAT;
			} else {
				return State.FORMAT_RANGE;
			}
		}
	}

	/**
	 *
	 * @param type - a type of the generator
	 * @param parameters - an array of parameters (if length > 1, first arg - a format, second - a range, by default)
	 * @return a suitable generator
	 */
	@Override @SuppressWarnings("unchecked")
	public G createGenerator(final char type, final String ... parameters) {
		final State state =  (State) defineState(parameters);
		final Matcher matcher;
		switch (state) {
			case EMPTY:
				switch (type) {
					case 'd':
						return (G) new AsyncLongGenerator(47L);
					default:
						throw new IllegalArgumentException();
				}
			case FORMAT:
				switch (type) {
					case 'f':
						return (G) new AsyncDoubleGenerator(47.0, parameters[0]);
					case 'D':
						try {
							return (G) new AsyncDateGenerator(
								parseDate("2016/02/25", INPUT_DATE_FMT_STRINGS), parameters[0]
							);
						} catch (ParseException e) {
							LogUtil.exception(
									LOG, Level.ERROR, e, "Failed to parse the pattern"
							);
						}
					default:
						throw new IllegalArgumentException();
				}
			case RANGE:
				switch (type) {
					case 'd':
						matcher = LONG_PATTERN.matcher(parameters[1]);
						if(matcher.find()) {
							return (G) new AsyncLongGenerator(
								Long.valueOf(matcher.group(1)), Long.valueOf(matcher.group(2))
							);
						} else {
							throw new IllegalArgumentException();
						}
					default:
						throw new IllegalArgumentException();
				}
			case FORMAT_RANGE:
				switch (type) {
					case 'f':
						matcher = DOUBLE_PATTERN.matcher(parameters[1]);
						if (matcher.find()) {
							return (G) new AsyncDoubleGenerator(
								Double.valueOf(matcher.group(1)), Double.valueOf(matcher.group(2)),
								parameters[0]
							);
						} else {
							throw new IllegalArgumentException();
						}
					case 'D':
						matcher = DATE_PATTERN.matcher(parameters[1]);
						if (matcher.find()) {
							try {
								return (G) new AsyncDateGenerator(
									parseDate(matcher.group(1), INPUT_DATE_FMT_STRINGS),
									parseDate(matcher.group(6), INPUT_DATE_FMT_STRINGS),
									parameters[0]
								);
							} catch (final ParseException e) {
								LogUtil.exception(
										LOG, Level.ERROR, e, "Failed to parse the pattern"
								);
							}
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
