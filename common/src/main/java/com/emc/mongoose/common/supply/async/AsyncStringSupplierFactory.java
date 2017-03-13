package com.emc.mongoose.common.supply.async;

import com.emc.mongoose.common.exception.DanShootHisFootException;
import com.emc.mongoose.common.supply.BatchSupplier;
import com.emc.mongoose.common.supply.SupplierFactory;


import java.text.ParseException;
import java.util.Date;
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
	 *
	 * @param type - a type of the generator
	 * @return a suitable generator
	 */
	@Override @SuppressWarnings("unchecked")
	public final G createSupplier(
		final char type, final String seedStr, final String formatStr, final String rangeStr
	) throws DanShootHisFootException {
		long seed = System.nanoTime() ^ System.currentTimeMillis();
		if(seedStr != null && !seedStr.isEmpty()) {
			try {
				seed = Long.parseLong(seedStr);
			} catch(final NumberFormatException e) {
				throw new DanShootHisFootException(
					"Seed value is not a 64 bit integer: \"" + seedStr + "\""
				);
			}
		}

		switch(type) {

			case 'd' : {
				long min = Long.MIN_VALUE;
				long max = Long.MAX_VALUE;
				if(rangeStr != null && !rangeStr.isEmpty()) {
					final Matcher matcher = LONG_PATTERN.matcher(rangeStr);
					if(matcher.find()) {
						min = Long.parseLong(matcher.group(1));
						max = Long.parseLong(matcher.group(2));
					} else {
						throw new DanShootHisFootException();
					}
				}
				return (G) new AsyncRangeDefinedLongFormattingSupplier(seed, min, max, formatStr);
			}

			case 'f' : {
				double min = 0;
				double max = 1;
				if(rangeStr != null && !rangeStr.isEmpty()) {
					final Matcher matcher = DOUBLE_PATTERN.matcher(rangeStr);
					if(matcher.find()) {
						min = Double.parseDouble(matcher.group(1));
						max = Double.parseDouble(matcher.group(2));
					} else {
						throw new DanShootHisFootException();
					}
				}
				return (G) new AsyncRangeDefinedDoubleFormattingSupplier(seed, min, max, formatStr);
			}

			case 'D': {
				Date min = new Date(0);
				Date max = new Date();
				if(rangeStr != null && !rangeStr.isEmpty()) {
					final Matcher matcher = DATE_PATTERN.matcher(rangeStr);
					if(matcher.find()) {
						try {
							min = parseDate(matcher.group(1), INPUT_DATE_FMT_STRINGS);
							max = parseDate(matcher.group(6), INPUT_DATE_FMT_STRINGS);
						} catch(final ParseException e) {
							throw new DanShootHisFootException("Failed to parse the pattern");
						}
					} else {
						throw new DanShootHisFootException();
					}
				}
				return (G) new AsyncRangeDefinedDateFormattingSupplier(seed, min, max, formatStr);
			}

			default:
				throw new DanShootHisFootException("Unknown format type: '" + type + "'");
		}
	}

}
