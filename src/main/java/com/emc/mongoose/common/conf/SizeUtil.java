package com.emc.mongoose.common.conf;
//
import com.emc.mongoose.common.log.LogUtil;
//
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 Created by andrey on 08.04.15.
 */
public final class SizeUtil {
	//
	private final static String
		SIZE_UNITS = "kmgtpe",
		FMT_MSG_INVALID_SIZE = "The string \"%s\" doesn't match the pattern: \"%s\"";
	private final static Pattern PATTERN_SIZE = Pattern.compile("(\\d+)(["+SIZE_UNITS+"]?)b?");
	//
	public static long toSize(final String value) {
		final String unit;
		final Matcher matcher = PATTERN_SIZE.matcher(value.toLowerCase());
		long size, degree;
		if(matcher.matches() && matcher.groupCount() > 0 && matcher.groupCount() < 3) {
			size = Long.valueOf(matcher.group(1), 10);
			unit = matcher.group(2);
			if(unit.length() == 0) {
				degree = 0;
			} else if(unit.length() == 1) {
				degree = SIZE_UNITS.indexOf(matcher.group(2)) + 1;
			} else {
				throw new IllegalArgumentException(
					String.format(FMT_MSG_INVALID_SIZE, value, PATTERN_SIZE)
				);
			}
		} else {
			throw new IllegalArgumentException(
				String.format(FMT_MSG_INVALID_SIZE, value, PATTERN_SIZE)
			);
		}
		size *= 1L << 10 * degree;
		return size;
	}
	//
	public static String formatSize(final long v) {
		if(v < 1024) {
			return v + "B";
		}
		final int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
		final double x = (double) v / (1L << (z * 10));
		return String.format(
			LogUtil.LOCALE_DEFAULT,
			x < 10 ? "%.3f%sb" : x < 100 ? "%.2f%sb" : "%.1f%sb",
			x, z > 0 ? SIZE_UNITS.charAt(z - 1) : ""
		).toUpperCase();
	}
	//
}
