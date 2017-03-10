package com.emc.mongoose.common.supply;

import com.emc.mongoose.common.exception.DanShootHisFootException;
import com.emc.mongoose.common.io.Input;

import java.text.ParseException;
import java.util.regex.Matcher;

import static org.apache.commons.lang.time.DateUtils.parseDate;

public class StringSupplierFactory<G extends BatchSupplier<String>>
implements SupplierFactory<String, G> {

	private static final StringSupplierFactory<? extends Input<String>>
		INSTANCE = new StringSupplierFactory<>();

	private StringSupplierFactory() {
	}

	public static StringSupplierFactory<? extends Input<String>> getInstance() {
		return INSTANCE;
	}
	
	@Override
	public State defineState(final String... parameters) {
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

	@Override @SuppressWarnings("unchecked")
	public G createSupplier(final char type, final String... parameters)
	throws DanShootHisFootException {
		final State state =  defineState(parameters);
		final Matcher matcher;
		switch (state) {
			case EMPTY:
				switch (type) {
					case 'd':
						return (G) new RangeDefinedLongFormattingSupplier();
					case 'f':
						return (G) new RangeDefinedDoubleFormattingSupplier();
					case 'D':
						return (G) new RangeDefinedDateFormattingSupplier();
					default:
						throw new DanShootHisFootException();
				}
			case FORMAT:
				switch (type) {
					case 'f':
						return (G) new RangeDefinedDoubleFormattingSupplier(parameters[0]);
					case 'D':
						return (G) new RangeDefinedDateFormattingSupplier(parameters[0]);
					case 'p':
						return (G) new FilePathSupplier(parameters[0]);
					default:
						throw new DanShootHisFootException();
				}
			case RANGE:
				switch(type) {
					case 'd':
						matcher = LONG_PATTERN.matcher(parameters[1]);
						if(matcher.find()) {
							return (G) new RangeDefinedLongFormattingSupplier(
								Long.parseLong(matcher.group(1)), Long.parseLong(matcher.group(2))
							);
						} else {
							throw new DanShootHisFootException();
						}
					case 'f':
						matcher = DOUBLE_PATTERN.matcher(parameters[1]);
						if(matcher.find()) {
							return (G) new RangeDefinedDoubleFormattingSupplier(
								Double.parseDouble(matcher.group(1)),
								Double.parseDouble(matcher.group(2))
							);
						} else {
							throw new DanShootHisFootException();
						}
					case 'D':
						matcher = DATE_PATTERN.matcher(parameters[1]);
						if(matcher.find()) {
							try {
								return (G) new RangeDefinedDateFormattingSupplier(
									parseDate(matcher.group(1), INPUT_DATE_FMT_STRINGS),
									parseDate(matcher.group(6), INPUT_DATE_FMT_STRINGS)
								);
							} catch(final ParseException e) {
								throw new DanShootHisFootException("Failed to parse the pattern");
							}
						} else {
							throw new DanShootHisFootException();
						}
					default:
						throw new DanShootHisFootException();
				}
			case FORMAT_RANGE:
				switch (type) {
					case 'd':
						matcher = LONG_PATTERN.matcher(parameters[1]);
						if(matcher.find()) {
							return (G) new RangeDefinedLongFormattingSupplier(
								Long.parseLong(matcher.group(1)), Long.parseLong(matcher.group(2)),
								parameters[0]
							);
						} else {
							throw new DanShootHisFootException();
						}
					case 'f':
						matcher = DOUBLE_PATTERN.matcher(parameters[1]);
						if(matcher.find()) {
							return (G) new RangeDefinedDoubleFormattingSupplier(
								Double.parseDouble(matcher.group(1)),
								Double.parseDouble(matcher.group(2)),
								parameters[0]
							);
						} else {
							throw new DanShootHisFootException();
						}
					case 'D':
						matcher = DATE_PATTERN.matcher(parameters[1]);
						if(matcher.find()) {
							try {
								return (G) new RangeDefinedDateFormattingSupplier(
									parseDate(matcher.group(1), INPUT_DATE_FMT_STRINGS),
									parseDate(matcher.group(6), INPUT_DATE_FMT_STRINGS),
									parameters[0]
								);
							} catch(final ParseException e) {
								throw new DanShootHisFootException("Failed to parse the pattern");
							}
						} else {
							throw new DanShootHisFootException();
						}
					default:
						throw new DanShootHisFootException();
				}
			default:
				throw new DanShootHisFootException();
		}
	}
}
