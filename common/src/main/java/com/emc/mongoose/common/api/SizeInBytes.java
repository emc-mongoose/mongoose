package com.emc.mongoose.common.api;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.emc.mongoose.common.Constants.LOCALE_DEFAULT;

/**
 Created by kurila on 10.02.16.
 */
public final class SizeInBytes
implements Serializable {
	
	private static final String
		FMT_MSG_INVALID_SIZE = "The string \"%s\" doesn't match the pattern: \"%s\"";
	
	public static final String SIZE_UNITS = "kmgtpe";
	public static final Pattern PATTERN_SIZE = Pattern.compile("([\\d\\.]+)(["+SIZE_UNITS+"]?)b?");
	
	public static long toFixedSize(final String value)
	throws NumberFormatException {
		final String unit;
		final Matcher matcher = PATTERN_SIZE.matcher(value.toLowerCase());
		double size;
		long degree;
		if(matcher.matches() && matcher.groupCount() > 0 && matcher.groupCount() < 3) {
			size = Double.parseDouble(matcher.group(1));
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
	
	public static String formatFixedSize(final long v) {
		if(v < 1024) {
			return v + "B";
		}
		final int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
		final double x = (double) v / (1L << (z * 10));
		if(x % 1 == 0) {
			final long y = (long) x;
			return String.format(
				LOCALE_DEFAULT,
				y < 10 ? "%d%sb" : y < 100 ? "%d%sb" : "%d%sb",
				y, z > 0 ? SIZE_UNITS.charAt(z - 1) : ""
			).toUpperCase();
		} else {
			return String.format(
				LOCALE_DEFAULT,
				x < 10 ? "%.3f%sb" : x < 100 ? "%.2f%sb" : "%.1f%sb",
				x, z > 0 ? SIZE_UNITS.charAt(z - 1) : ""
			).toUpperCase();
		}
	}

	private static final char SEP1 = '-', SEP2 = ',';
	
	private long min, range = 0;
	private double bias = 1;

	public SizeInBytes(final String sizeInfo) {
		final int
			sep1pos = sizeInfo.indexOf(SEP1, 0),
			sep2pos = sizeInfo.indexOf(SEP2, 0);
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
	
	public SizeInBytes(final long size) {
		this(size, size, 1);
	}
	
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

	public SizeInBytes(final SizeInBytes other) {
		this.min = other.min;
		this.range = other.range;
		this.bias = other.bias;
	}
	
	public long get() {
		if(range == 0) {
			return min;
		} else if(bias == 1) {
			return min + ThreadLocalRandom.current().nextLong(range + 1);
		} else {
			return min + (long) Math.pow(ThreadLocalRandom.current().nextDouble(), bias) * range;
		}
	}
	
	public long getMin() {
		return min;
	}
	
	public long getMax() {
		return min + range;
	}
	
	private static final int APPROXIMATION_COUNT = 100;
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
	
	private static final ThreadLocal<StringBuilder>
		STRING_BULDER = new ThreadLocal<StringBuilder>() {
			@Override
			protected final StringBuilder initialValue() {
				return new StringBuilder();
			}
		};
	@Override
	public final String toString() {
		final StringBuilder sb = STRING_BULDER.get();
		sb.setLength(0);
		sb.append(formatFixedSize(min));
		if(range > 0) {
			sb.append(SEP1).append(formatFixedSize(min + range));
			if(bias != 1) {
				sb.append(SEP2).append(Double.toString(bias));
			}
		}
		return sb.toString();
	}
	
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
