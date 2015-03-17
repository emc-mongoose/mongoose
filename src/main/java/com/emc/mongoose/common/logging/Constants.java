package com.emc.mongoose.common.logging;
//
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
/**
 Created by kurila on 17.03.15.
 */
public interface Constants {
	TimeZone TZ_UTC = TimeZone.getTimeZone("UTC");
	Locale LOCALE_DEFAULT = Locale.ROOT;
	DateFormat FMT_DT = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS", LOCALE_DEFAULT) {
		{
			setTimeZone(TZ_UTC);
		}
	};
}
