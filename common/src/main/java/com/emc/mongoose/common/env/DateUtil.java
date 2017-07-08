package com.emc.mongoose.common.env;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static com.emc.mongoose.common.Constants.LOCALE_DEFAULT;

/**
 Created by andrey on 18.11.16.
 */
public interface DateUtil {

	TimeZone TZ_UTC = TimeZone.getTimeZone("UTC");

	String PATTERN_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss,SSS";
	DateFormat FMT_DATE_ISO8601 = new SimpleDateFormat(PATTERN_ISO8601, LOCALE_DEFAULT) {{
		setTimeZone(TZ_UTC);
	}};

	//e.g. Sun, 06 Nov 1994 08:49:37 GMT
	String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
	DateFormat FMT_DATE_RFC1123 = new SimpleDateFormat(PATTERN_RFC1123, LOCALE_DEFAULT) {{
		setTimeZone(TZ_UTC);
	}};

	String PATTERN_METRICS_TABLE = "yyyyMMdd-HHmmss";
	DateFormat FMT_DATE_METRICS_TABLE = new SimpleDateFormat(PATTERN_METRICS_TABLE, LOCALE_DEFAULT) {{
		setTimeZone(TZ_UTC);
	}};
}
