package com.emc.mongoose.common.api;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 Created by andrey on 07.04.15.
 */
public abstract class TimeUtil {
	
	private final static Map<String, TimeUnit> TIME_UNIT_SHORTCUTS = new HashMap<String, TimeUnit>() {
		{
			put("s", TimeUnit.SECONDS);
			put("m", TimeUnit.MINUTES);
			put("h", TimeUnit.HOURS);
			put("d", TimeUnit.DAYS);
		}
	};
	private final static Pattern PATTERN_TIME = Pattern.compile("([0-9]*)([smhdSMHD]?)");
	private final static Pattern PATTERN_TIME_COMPAT = Pattern.compile("([0-9]*)\\.([a-zA-Z]{4,7})");
	
	public static long getTimeValue(final String rawValue)
	throws IllegalArgumentException {
		
		final String timeValueSpec;
		Matcher m = PATTERN_TIME.matcher(rawValue);
		
		if(m.matches()) {
			timeValueSpec = m.group(1);
		} else {
			m = PATTERN_TIME_COMPAT.matcher(rawValue);
			if(m.matches()) {
				timeValueSpec = m.group(1);
			} else {
				throw new IllegalArgumentException(
					String.format(
						"Time value \"%s\" doesn't match the pattern \"%s\"", rawValue,
						PATTERN_TIME.pattern()
					)
				);
			}
		}
		
		try {
			return Long.valueOf(timeValueSpec);
		} catch(final NumberFormatException e) {
			return 0;
		}
	}
	
	public static TimeUnit getTimeUnit(final String rawValue) {
		
		TimeUnit result = TimeUnit.SECONDS;
		Matcher m = PATTERN_TIME.matcher(rawValue);
		
		if(m.matches()) {
			final String t = m.group(2).toLowerCase();
			if(TIME_UNIT_SHORTCUTS.containsKey(t)) {
				result = TIME_UNIT_SHORTCUTS.get(t);
			}
		} else {
			m = PATTERN_TIME_COMPAT.matcher(rawValue);
			if(m.matches()) {
				result = TimeUnit.valueOf(m.group(2).toUpperCase());
			} else {
				throw new IllegalArgumentException(
					String.format(
						"Time value \"%s\" doesn't match the pattern \"%s\"", rawValue,
						PATTERN_TIME.pattern()
					)
				);
			}
		}
		
		return result;
	}
	
	public static long getTimeInSeconds(final String rawValue) {
		final TimeUnit timeUnit = TimeUtil.getTimeUnit(rawValue);
		if(timeUnit == null) {
			return TimeUtil.getTimeValue(rawValue);
		} else {
			return timeUnit.toSeconds(TimeUtil.getTimeValue(rawValue));
		}
	}
}
