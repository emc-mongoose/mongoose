package com.emc.mongoose.common.supply.async;

import com.emc.mongoose.common.exception.DanShootHisFootException;
import com.emc.mongoose.common.supply.BatchSupplier;
import com.emc.mongoose.common.supply.SupplierFactory;
import com.emc.mongoose.common.supply.async.range.AsyncRangeDefinedDateSupplier;
import com.emc.mongoose.common.supply.async.range.AsyncRangeDefinedDoubleSupplier;
import com.emc.mongoose.common.supply.async.range.AsyncRangeDefinedLongSupplier;

import static com.emc.mongoose.common.supply.async.range.AsyncRangeDefinedDateSupplier.INPUT_DATE_FMT_STRINGS;
import static com.emc.mongoose.common.supply.range.RangeDefinedSupplier.RANGE_DELIMITER;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.time.DateUtils.parseDate;

public final class AsyncStringSupplierFactory<G extends BatchSupplier<String>>
implements SupplierFactory<String, G> {

	private static final Pattern DOUBLE_PATTERN = Pattern.compile(rangeRegExp(DOUBLE_REG_EXP));
	private static final Pattern LONG_PATTERN = Pattern.compile(rangeRegExp(LONG_REG_EXP));
	private static final Pattern DATE_PATTERN = Pattern.compile(rangeRegExp(DATE_REG_EXP));

	private static final AsyncStringSupplierFactory<? extends BatchSupplier<String>>
			INSTANCE = new AsyncStringSupplierFactory<>();

	private AsyncStringSupplierFactory() {
	}

	public static AsyncStringSupplierFactory<? extends BatchSupplier<String>> getInstance() {
		return INSTANCE;
	}

	// Pay attention to the escape symbols
	private static String rangeRegExp(final String typeRegExp) {
		return typeRegExp + RANGE_DELIMITER + typeRegExp;
	}

	/**
	 * This enum is used to ease understanding of switch-case in createSupplier() method.
	 * @param parameters :
	 * [0] an output format for an AsyncFormatRangeGeneratorBase generator
	 * [1] a range of random generator AsyncRangeGeneratorBase values
	 * @return a state that defines a choice of the generator by the factory
	 */
	@Override
	public final State defineState(final String... parameters) {
		if(parameters[0] == null) {
			if(parameters[1] == null) {
				return State.EMPTY;
			} else {
				return State.RANGE;
			}
		} else {
			if(parameters[1] == null) {
				return State.FORMAT;
			} else {
				return State.FORMAT_RANGE;
			}
		}
	}

	/**
	 *
	 * @param type - a type of the generator
	 * @param parameters - an array of parameters (if length &gt; 1, first arg - a format, second - a range, by default)
	 * @return a suitable generator
	 */
	@Override @SuppressWarnings("unchecked")
	public final G createSupplier(final char type, final String ... parameters)
	throws DanShootHisFootException {
		final State state =  (State) defineState(parameters);
		final Matcher matcher;
		switch (state) {
			case EMPTY:
				switch (type) {
					case 'd':
						return (G) new AsyncRangeDefinedLongSupplier(47L);
					default:
						throw new DanShootHisFootException();
				}
			case FORMAT:
				switch (type) {
					case 'f':
						return (G) new AsyncRangeDefinedDoubleSupplier(47.0, parameters[0]);
					case 'D':
						try {
							return (G) new AsyncRangeDefinedDateSupplier(
								parseDate("2016/02/25", INPUT_DATE_FMT_STRINGS), parameters[0]
							);
						} catch(final ParseException e) {
							throw new DanShootHisFootException("Failed to parse the pattern");
						}
					default:
						throw new IllegalArgumentException();
				}
			case RANGE:
				switch (type) {
					case 'd':
						matcher = LONG_PATTERN.matcher(parameters[1]);
						if(matcher.find()) {
							return (G) new AsyncRangeDefinedLongSupplier(
								Long.parseLong(matcher.group(1)), Long.parseLong(matcher.group(2))
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
						if(matcher.find()) {
							return (G) new AsyncRangeDefinedDoubleSupplier(
								Double.parseDouble(matcher.group(1)),
								Double.parseDouble(matcher.group(2)),
								parameters[0]
							);
						} else {
							throw new IllegalArgumentException();
						}
					case 'D':
						matcher = DATE_PATTERN.matcher(parameters[1]);
						if(matcher.find()) {
							try {
								return (G) new AsyncRangeDefinedDateSupplier(
									parseDate(matcher.group(1), INPUT_DATE_FMT_STRINGS),
									parseDate(matcher.group(6), INPUT_DATE_FMT_STRINGS),
									parameters[0]
								);
							} catch(final ParseException e) {
								throw new DanShootHisFootException("Failed to parse the pattern");
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
