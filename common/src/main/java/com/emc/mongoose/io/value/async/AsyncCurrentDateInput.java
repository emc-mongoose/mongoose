package com.emc.mongoose.io.value.async;

import com.emc.mongoose.log.LogUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 Created by kurila on 16.04.15.
 */
public final class AsyncCurrentDateInput
extends AsyncValueInput<String> {
	//
	public final static String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz"; //e.g. Sun, 06 Nov 1994 08:49:37 GMT
	public final static DateFormat FMT_DATE = new SimpleDateFormat(
		PATTERN_RFC1123, LogUtil.LOCALE_DEFAULT
	) {{
		setTimeZone(LogUtil.TZ_UTC);
	}};
	public final static AsyncCurrentDateInput INSTANCE = new AsyncCurrentDateInput();
	//
	private
	AsyncCurrentDateInput() {
		super(
			FMT_DATE.format(new Date(System.currentTimeMillis())),
			new InitializedCallableBase<String>() {
				//
				@Override
				public final String call()
				throws Exception {
					return FMT_DATE.format(new Date(System.currentTimeMillis()));
				}
			}
		);
	}
}
