package com.emc.mongoose.model.util;

import java.io.Serializable;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 Created by kurila on 10.02.16.
 */
public final class SizeInBytes
implements Serializable {
	//
	private final static String
		FMT_MSG_INVALID_SIZE = "The string \"%s\" doesn't match the pattern: \"%s\"";
	//
	public final static String SIZE_UNITS = "kmgtpe";
	public final static Pattern PATTERN_SIZE = Pattern.compile("([\\d\\.]+)(["+SIZE_UNITS+"]?)b?");
	//
	public static long toFixedSize(final String value)
	throws NumberFormatException {
		final String unit;
		final Matcher matcher = PATTERN_SIZE.matcher(value.toLowerCase());
		double size;
		long degree;
		if(matcher.matches() && matcher.groupCount() > 0 && matcher.groupCount() < 3) {
			size = Double.valueOf(matcher.group(1));
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
		return (long) size;
	}
	//
	public static String formatFixedSize(final long v) {
		if(v < 1024) {
			return v + "B";
		}
		final int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
		final double x = (double) v / (1L << (z * 10));
		if(x % 1 == 0) {
			final long y = (long) x;
			return String.format(
				Locale.ROOT,
				y < 10 ? "%d%sb" : y < 100 ? "%d%sb" : "%d%sb",
				y, z > 0 ? SIZE_UNITS.charAt(z - 1) : ""
			).toUpperCase();
		} else {
			return String.format(
				Locale.ROOT,
				x < 10 ? "%.3f%sb" : x < 100 ? "%.2f%sb" : "%.1f%sb",
				x, z > 0 ? SIZE_UNITS.charAt(z - 1) : ""
			).toUpperCase();
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static char SEP1 = '-', SEP2 = ',';
	//
	private long min, range = 0;
	private double bias = 1;
	//
	public SizeInBytes(final String sizeInfo) {
		final int
			sep1pos = sizeInfo.indexOf(SEP1),
			sep2pos = sizeInfo.indexOf(SEP2);
		if(sep1pos < 0) {
			min = toFixedSize(sizeInfo);
		} else {
			min = toFixedSize(sizeInfo.substring(0, sep1pos));
			if(sep2pos < 0) {
				range = toFixedSize(sizeInfo.substring(sep1pos + 1)) - min;
			} else {
				range = toFixedSize(sizeInfo.substring(sep1pos + 1, sep2pos)) - min;
				bias = Double.parseDouble(sizeInfo.substring(sep2pos + 1));
			}
		}
		if(range < 0) {
			throw new IllegalArgumentException("Min value is less than max: \"" + sizeInfo + "\"");
		}
	}
	//
	public SizeInBytes(final long size) {
		this(size, size, 1);
	}
	//
	public SizeInBytes(final long min, final long max, final double bias) {
		if(min < 0) {
			throw new IllegalArgumentException("Min size is less than 0");
		}
		if(min > max) {
			throw new IllegalArgumentException("Min size is more than max");
		}
		if(max < 0) {
			throw new IllegalArgumentException("Max size is less than 0");
		}
		this.min = min;
		this.range = max - min;
		this.bias = bias;
	}
	//
	public long get() {
		if(range == 0) {
			return min;
		} else if(bias == 1) {
			return min + ThreadLocalRandom.current().nextLong(range + 1);
		} else {
			return min + (long) Math.pow(ThreadLocalRandom.current().nextDouble(), bias) * range;
		}
	}
	//
	public long getMin() {
		return min;
	}
	//
	public long getMax() {
		return min + range;
	}
	//
	private final static int APPROXIMATION_COUNT = 100;
	public final long getAvg() {
		if(range == 0) {
			return min;
		} else {
			long sum = 0;
			for(int i = 0; i < APPROXIMATION_COUNT; i ++) {
				sum += get();
			}
			return sum / APPROXIMATION_COUNT;
		}
	}
	//
	@Override
	public final String toString() {
		final StringBuilder sb = new StringBuilder(formatFixedSize(min));
		if(range > 0) {
			sb.append(SEP1).append(formatFixedSize(min + range));
			if(bias != 1) {
				sb.append(SEP2).append(Double.toString(bias));
			}
		}
		return sb.toString();
	}
	//
	@Override
	public final boolean equals(final Object o) {
		if(o instanceof SizeInBytes) {
			final SizeInBytes s = (SizeInBytes) o;
			return min == s.min && range == s.range && bias == s.bias;
		} else {
			return false;
		}
	}
}
