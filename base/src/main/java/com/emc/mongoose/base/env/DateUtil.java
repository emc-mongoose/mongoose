package com.emc.mongoose.base.env;

import com.emc.mongoose.base.Constants;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** Created by andrey on 18.11.16. */
public interface DateUtil {

	TimeZone TZ_UTC = TimeZone.getTimeZone("UTC");
	String PATTERN_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss,SSS";
	DateFormat FMT_DATE_ISO8601 = new SimpleDateFormat(PATTERN_ISO8601, Constants.LOCALE_DEFAULT) {
		{
			setTimeZone(TZ_UTC);
		}
	};

	static String formatNowIso8601() {
		return FMT_DATE_ISO8601.format(new Date(System.currentTimeMillis()));
	}

	// e.g. Sun, 06 Nov 1994 08:49:37 GMT
	String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
	DateFormat FMT_DATE_RFC1123 = new SimpleDateFormat(PATTERN_RFC1123, Constants.LOCALE_DEFAULT) {
		{
			setTimeZone(TZ_UTC);
		}
	};

	static String formatNowRfc1123() {
		return FMT_DATE_RFC1123.format(new Date(System.currentTimeMillis()));
	}

	String PATTERN_METRICS_TABLE = "yyMMddHHmmss";
	DateFormat FMT_DATE_METRICS_TABLE = new SimpleDateFormat(PATTERN_METRICS_TABLE, Constants.LOCALE_DEFAULT) {
		{
			setTimeZone(TZ_UTC);
		}
	};

	ThreadLocal<Map<String, DateFormat>> DATE_FORMATS = ThreadLocal.withInitial(HashMap::new);

	static DateFormat dateFormat(final String pattern) {
		return DATE_FORMATS.get().computeIfAbsent(
						pattern,
						p -> {
							final var f = new SimpleDateFormat(p, Locale.ROOT);
							f.setTimeZone(TZ_UTC);
							return f;
						});
	}

	static Date date(long millisSinceEpoch) {
		return new Date(millisSinceEpoch);
	}

	static long toMillisSinceEpoch(final Date date) {
		return date.getTime();
	}
}
