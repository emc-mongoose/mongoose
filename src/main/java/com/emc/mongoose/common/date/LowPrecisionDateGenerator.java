package com.emc.mongoose.common.date;
//
import com.emc.mongoose.common.logging.LogUtil;
//
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
/**
 Created by kurila on 16.04.15.
 */
public class LowPrecisionDateGenerator {
	//
	public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
	public static final DateFormat FMT_DATE = new SimpleDateFormat(
		PATTERN_RFC1123, LogUtil.LOCALE_DEFAULT
	) {{
		setTimeZone(LogUtil.TZ_UTC);
	}};
	//
	private static long DT_MILLISEC = System.currentTimeMillis();
	private static AtomicReference<String> DT_TEXT_REF = new AtomicReference<>(
		FMT_DATE.format(new Date(DT_MILLISEC))
	);
	//
	public static int UPDATE_PERIOD_MILLISEC = 100000;
	//
	private static final Timer UPDATE_DAEMON = new Timer(true) {{
		schedule(
			new TimerTask() {
				@Override
				public void run() {
					DT_MILLISEC = System.currentTimeMillis();
					DT_TEXT_REF.set(FMT_DATE.format(new Date(DT_MILLISEC)));
				}
			}, UPDATE_PERIOD_MILLISEC, UPDATE_PERIOD_MILLISEC
		);
	}};
	//
	public static String getDateText() {
		return DT_TEXT_REF.get();
	}
}
