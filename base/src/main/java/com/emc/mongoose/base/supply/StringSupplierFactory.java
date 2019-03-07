package com.emc.mongoose.base.supply;

import static org.apache.commons.lang.time.DateUtils.parseDate;

import com.emc.mongoose.base.exception.OmgShootMyFootException;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;

public final class StringSupplierFactory<G extends BatchSupplier<String>>
				implements SupplierFactory<String, G> {

	private static final StringSupplierFactory<? extends BatchSupplier<String>> INSTANCE = new StringSupplierFactory<>();

	private StringSupplierFactory() {}

	public static StringSupplierFactory<? extends BatchSupplier<String>> getInstance() {
		return INSTANCE;
	}

	@Override
	public G createSupplier(
					final char type, final String seedStr, final String formatStr, final String rangeStr)
					throws OmgShootMyFootException {

		long seed = System.nanoTime() ^ System.currentTimeMillis();
		if (seedStr != null && !seedStr.isEmpty()) {
			try {
				seed = Long.parseLong(seedStr);
			} catch (final NumberFormatException e) {
				throw new OmgShootMyFootException(
								"Seed value is not a 64 bit integer: \"" + seedStr + "\"");
			}
		}

		switch (type) {
		case 'd': {
			long min = Long.MIN_VALUE;
			long max = Long.MAX_VALUE;
			if (rangeStr != null && !rangeStr.isEmpty()) {
				final Matcher matcher = LONG_PATTERN.matcher(rangeStr);
				if (matcher.find()) {
					min = Long.parseLong(matcher.group(1));
					max = Long.parseLong(matcher.group(2));
				} else {
					throw new OmgShootMyFootException();
				}
			}
			return (G) new RangeDefinedLongFormattingSupplier(seed, min, max, formatStr);
		}

		case 'f': {
			double min = 0;
			double max = 1;
			if (rangeStr != null && !rangeStr.isEmpty()) {
				final Matcher matcher = DOUBLE_PATTERN.matcher(rangeStr);
				if (matcher.find()) {
					min = Double.parseDouble(matcher.group(1));
					max = Double.parseDouble(matcher.group(2));
				} else {
					throw new OmgShootMyFootException();
				}
			}
			return (G) new RangeDefinedDoubleFormattingSupplier(seed, min, max, formatStr);
		}

		case 'D': {
			Date min = new Date(0);
			Date max = new Date();
			if (rangeStr != null && !rangeStr.isEmpty()) {
				final Matcher matcher = DATE_PATTERN.matcher(rangeStr);
				if (matcher.find()) {
					try {
						min = parseDate(matcher.group(1), INPUT_DATE_FMT_STRINGS);
						max = parseDate(matcher.group(6), INPUT_DATE_FMT_STRINGS);
					} catch (final ParseException e) {
						throw new OmgShootMyFootException("Failed to parse the pattern");
					}
				} else {
					throw new OmgShootMyFootException();
				}
			}
			return (G) new RangeDefinedDateFormattingSupplier(seed, min, max, formatStr);
		}

		case 'p':
			return (G) new FilePathSupplier(seed, formatStr);

		default:
			throw new OmgShootMyFootException("Unknown format type: '" + type + "'");
		}
	}
}
