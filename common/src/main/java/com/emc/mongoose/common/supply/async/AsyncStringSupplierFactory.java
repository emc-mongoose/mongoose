package com.emc.mongoose.common.supply.async;

import com.emc.mongoose.common.exception.DanShootHisFootException;
import com.emc.mongoose.common.supply.BatchSupplier;
import com.emc.mongoose.common.supply.SupplierFactory;


import java.text.ParseException;
import java.util.regex.Matcher;

import static org.apache.commons.lang.time.DateUtils.parseDate;

public final class AsyncStringSupplierFactory<G extends BatchSupplier<String>>
implements SupplierFactory<String, G> {

	private static final AsyncStringSupplierFactory<? extends BatchSupplier<String>>
		INSTANCE = new AsyncStringSupplierFactory<>();

	private AsyncStringSupplierFactory() {
	}

	public static AsyncStringSupplierFactory<? extends BatchSupplier<String>> getInstance() {
		return INSTANCE;
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
		final State state =  defineState(parameters);
		final Matcher matcher;
		switch (state) {
			case EMPTY:
				switch (type) {
					case 'd':
						return (G) new AsyncRangeDefinedLongFormattingSupplier();
					case 'f':
						return (G) new AsyncRangeDefinedDoubleFormattingSupplier();
					case 'D':
						return (G) new AsyncRangeDefinedDateFormattingSupplier();
					default:
						throw new DanShootHisFootException();
				}
			case FORMAT:
				switch (type) {
					case 'd':
						return (G) new AsyncRangeDefinedLongFormattingSupplier(parameters[0]);
					case 'f':
						return (G) new AsyncRangeDefinedDoubleFormattingSupplier(parameters[0]);
					case 'D':
						return (G) new AsyncRangeDefinedDateFormattingSupplier(parameters[0]);
					default:
						throw new IllegalArgumentException();
				}
			case RANGE:
				switch (type) {
					case 'd':
						matcher = LONG_PATTERN.matcher(parameters[1]);
						if(matcher.find()) {
							return (G) new AsyncRangeDefinedLongFormattingSupplier(
								Long.parseLong(matcher.group(1)), Long.parseLong(matcher.group(2))
							);
						} else {
							throw new IllegalArgumentException();
						}
					case 'f':
						matcher = DOUBLE_PATTERN.matcher(parameters[1]);
						if(matcher.find()) {
							return (G) new AsyncRangeDefinedDoubleFormattingSupplier(
								Double.parseDouble(matcher.group(1)),
								Double.parseDouble(matcher.group(2))
							);
						} else {
							throw new IllegalArgumentException();
						}
					case 'D':
						matcher = DATE_PATTERN.matcher(parameters[1]);
						if(matcher.find()) {
							try {
								return (G) new AsyncRangeDefinedDateFormattingSupplier(
									parseDate(matcher.group(1), INPUT_DATE_FMT_STRINGS),
									parseDate(matcher.group(6), INPUT_DATE_FMT_STRINGS)
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
			case FORMAT_RANGE:
				switch (type) {
					case 'd':
						matcher = LONG_PATTERN.matcher(parameters[1]);
						if(matcher.find()) {
							return (G) new AsyncRangeDefinedLongFormattingSupplier(
								Long.parseLong(matcher.group(1)), Long.parseLong(matcher.group(2)),
								parameters[0]
							);
						} else {
							throw new IllegalArgumentException();
						}
					case 'f':
						matcher = DOUBLE_PATTERN.matcher(parameters[1]);
						if(matcher.find()) {
							return (G) new AsyncRangeDefinedDoubleFormattingSupplier(
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
								return (G) new AsyncRangeDefinedDateFormattingSupplier(
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
