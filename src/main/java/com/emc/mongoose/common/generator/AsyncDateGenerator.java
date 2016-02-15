package com.emc.mongoose.common.generator;
//
import com.emc.mongoose.common.log.LogUtil;
//
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
/**
 Created by kurila on 16.04.15.
 */
public final class AsyncDateGenerator
extends AsyncValueGenerator<String> {
	//
	public final static String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
	public final static DateFormat FMT_DATE = new SimpleDateFormat(
		PATTERN_RFC1123, LogUtil.LOCALE_DEFAULT
	) {{
		setTimeZone(LogUtil.TZ_UTC);
	}};
	public final static AsyncDateGenerator INSTANCE = new AsyncDateGenerator();
	//
	private AsyncDateGenerator() {
		super(
			FMT_DATE.format(new Date(System.currentTimeMillis())),
			new Callable<String>() {
				@Override
				public final String call()
				throws Exception {
					return FMT_DATE.format(new Date(System.currentTimeMillis()));
				}
			}
		);
	}
}
