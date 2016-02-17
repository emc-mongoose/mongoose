package com.emc.mongoose.common.net.http.request.format;

import com.emc.mongoose.common.generator.*;

import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.emc.mongoose.common.generator.AsyncDateGenerator.FMT_DATE;

public class HeaderValueGeneratorFactory {

	// pay attention to the matcher groups
	private static final String doubleRegExp = "([-+]?\\d*\\.?\\d+)";
	private static final String longRegExp = "([-+]?\\d+)";
	private static final String dateRegExp = "(((19|20)[0-9][0-9])/(0?[1-9]|1[012])/(0?[1-9]|[12][0-9]|3[01]))";
	private static final Pattern doublePattern = Pattern.compile(rangeRegExp(doubleRegExp));
	private static final Pattern longPattern = Pattern.compile(rangeRegExp(longRegExp));
	private static final Pattern datePattern = Pattern.compile(rangeRegExp(dateRegExp));

	private HeaderValueGeneratorFactory() {
	}

	// Pay attention to the escape symbols
	private static String rangeRegExp(String typeRegExp) {
		return typeRegExp + HeaderFormatter.RANGE_DELIMITER + typeRegExp;
	}

	public static ValueGenerator createGenerator(char type)
	throws ParseException {
		switch (type) {
			case 'f':
				return new AsyncDoubleGenerator(47.0);
			case 'd':
				return new AsyncLongGenerator(47L);
			case 'D':
				return new AsyncStringDateGenerator(FMT_DATE.format(new Date()));
			default:
				return null;
		}
	}

	public static ValueGenerator createGenerator(char type, String range)
	throws IllegalArgumentException, ParseException {
		Matcher matcher;
		switch (type) {
			case 'f':
				matcher = doublePattern.matcher(range);
				if (matcher.find()) {
					return new AsyncDoubleGenerator(Double.valueOf(matcher.group(1)), Double.valueOf(matcher.group(2)));
				} else {
					throw new IllegalArgumentException();
				}
			case 'd':
				matcher = longPattern.matcher(range);
				if (matcher.find()) {
					return new AsyncLongGenerator(Long.valueOf(matcher.group(1)), Long.valueOf(matcher.group(2)));
				} else {
					throw new IllegalArgumentException();
				}
			case 'D':
				matcher = datePattern.matcher(range);
				if (matcher.find()) {
					return new AsyncStringDateGenerator(matcher.group(1), matcher.group(6));
				} else {
					throw new IllegalArgumentException();
				}
			default:
				return null;
		}
	}
}
