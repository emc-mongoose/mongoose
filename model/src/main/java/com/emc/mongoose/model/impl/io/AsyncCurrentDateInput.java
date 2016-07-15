package com.emc.mongoose.model.impl.io;

import com.emc.mongoose.common.exception.OmgDoesNotPerformException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 Created by kurila on 16.04.15.
 */
public final class AsyncCurrentDateInput
extends AsyncValueInput<String> {
	//
	public final static String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz"; //e.g. Sun, 06 Nov 1994 08:49:37 GMT
	public final static DateFormat FMT_DATE = new SimpleDateFormat(
		PATTERN_RFC1123, Locale.ROOT
	) {{
		setTimeZone(TimeZone.getTimeZone("UTC"));
	}};
	public static AsyncCurrentDateInput INSTANCE = null;
	static {
		try {
			INSTANCE = new AsyncCurrentDateInput();
		} catch(final OmgDoesNotPerformException e) {
			e.printStackTrace(System.err);
		}
	}
	//
	private AsyncCurrentDateInput()
	throws OmgDoesNotPerformException {
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
