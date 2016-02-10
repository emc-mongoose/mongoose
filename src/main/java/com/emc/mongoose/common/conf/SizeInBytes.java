package com.emc.mongoose.common.conf;
import com.emc.mongoose.common.log.LogUtil;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 Created by kurila on 10.02.16.
 */
public final class SizeInBytes {
	//
	private final static String
		FMT_MSG_INVALID_SIZE = "The string \"%s\" doesn't match the pattern: \"%s\"";
	//
	public final static String SIZE_UNITS = "kmgtpe";
	public final static Pattern PATTERN_SIZE = Pattern.compile("(\\d+)(["+SIZE_UNITS+"]?)b?");
	//
	public static long toFixedSize(final String value) {
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
	public static String formatFixedSize(final long v) {
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
	////////////////////////////////////////////////////////////////////////////////////////////////
	private long min, max;
	private double bias;
	//
	public SizeInBytes(final String sizeInfo) {
		final String bounds[] = sizeInfo.split("-");
		if(bounds.length == 2) {
			min = toFixedSize(bounds[0]);
			max = toFixedSize(bounds[1]);
		} else {
			min = max = toFixedSize(sizeInfo);
		}
		bias = 1;
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
		this.max = max;
		this.bias = bias;
	}
	//
	public long get() {
		return ThreadLocalRandom.current().nextLong(min, max + 1);
	}
	//
	private final static int APPROXIMATION_COUNT = 100;
	public final long getAvgDataSize() {
		if(min == max) {
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
		if(min == max) {
			return formatFixedSize(min);
		} else {
			return formatFixedSize(min) + "-" + formatFixedSize(max);
		}
	}
}
