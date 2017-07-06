package com.emc.mongoose.ui.log;

import com.emc.mongoose.model.DaemonBase;

import static com.emc.mongoose.common.Constants.KEY_BASE_DIR;
import static com.emc.mongoose.common.Constants.KEY_TEST_ID;
import static com.emc.mongoose.common.Constants.LOCALE_DEFAULT;
import static com.emc.mongoose.common.env.DateUtil.TZ_UTC;
import static com.emc.mongoose.common.env.PathUtil.BASE_DIR;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.core.util.datetime.DatePrinter;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;

import java.util.Calendar;

/**
 Created by kurila on 06.05.14.
 */
public final class LogUtil
implements ShutdownCallbackRegistry {
	//
	public static final DatePrinter
		FMT_DT = FastDateFormat.getInstance("yyyyMMdd.HHmmss.SSS", TZ_UTC, LOCALE_DEFAULT);
	// console colors
	public static final String
		RED = "\u001B[31m",
		GREEN = "\u001B[32m",
		YELLOW = "\u001B[33",
		BLUE = "\u001B[34m",
		CYAN = "\u001B[36m",
		WHITE = "\u001B[37;1m",
		RESET = "\u001B[0m",
		//
		NOOP_COLOR = "\u001B[38;5;101m",
		CREATE_COLOR = "\u001B[38;5;67m",
		READ_COLOR = "\u001B[38;5;65m",
		UPDATE_COLOR = "\u001B[38;5;104m",
		DELETE_COLOR = "\u001B[38;5;137m",
		LIST_COLOR = "\u001B[38;5;138m";
	//
	public static String getDateTimeStamp() {
		return FMT_DT.format(
			Calendar.getInstance(TZ_UTC, LOCALE_DEFAULT).getTime()
		);
	}
	//
	public static void init() {
		ThreadContext.put(KEY_BASE_DIR, BASE_DIR);
		ThreadContext.put(KEY_TEST_ID, getDateTimeStamp());
		try {
			Runtime.getRuntime().addShutdownHook(
				new Thread("logCtxShutDownHook") {
					@Override
					public final void run() {
						shutdown();
					}
				}
			);
		} catch(final Exception e) {
			e.printStackTrace(System.err);
		}
	}
	//
	public static void flushAll() {
		final LoggerContext logCtx = ((LoggerContext) LogManager.getContext());
		for(final org.apache.logging.log4j.core.Logger logger : logCtx.getLoggers()) {
			for(final Appender appender : logger.getAppenders().values()) {
				if(appender instanceof AbstractOutputStreamAppender) {
					((AbstractOutputStreamAppender) appender).getManager().flush();
				}
			}
		}
	}
	//
	public static void shutdown() {
		try {
			DaemonBase.closeAll();
			LogManager.shutdown();
		} catch(final Throwable cause) {
			cause.printStackTrace(System.err);
		}
	}
	//
	public static String getFailureRatioAnsiColorCode(final long succ, final long fail) {
		if(fail == 0) {
			return "\u001B[38;2;0;200;0m";
		}
		if(fail >= succ) {
			return "\u001B[38;2;" + ((int) (200 + ((double) 55 * fail) / (succ + fail))) + ";0;0m";
		}
		return "\u001B[38;2;" +
			/* R */ ((int) (400 * Math.sqrt(((double) fail) / (succ + fail)))) + ";" +
			/* G */ ((int) (((double) 200 * succ / (succ + fail)))) + ";" +
			/* B */ "0m";
	}
	//
	public static void exception(
		final Level level, final Throwable e,
		final String msgPattern, final Object... args
	) {
		if(Loggers.ERR.isTraceEnabled()) {
			Loggers.ERR.log(
				level, Loggers.ERR.getMessageFactory().newMessage(msgPattern + ": " + e, args), e
			);
		} else {
			Loggers.ERR.log(
				level, Loggers.ERR.getMessageFactory().newMessage(msgPattern + ": " + e, args)
			);
		}
	}
	//
	public static void trace(
		final Logger logger, final Level level, final String msgPattern, final Object... args
	) {
		logger.log(level, logger.getMessageFactory().newMessage(msgPattern, args), new Throwable());
	}

	@Override
	public final Cancellable addShutdownCallback(final Runnable callback) {
		return new Cancellable() {
			@Override
			public final void cancel() {
			}
			@Override
			public final void run() {
				if(callback != null) {
					System.out.println("Shutdown callback + \"" + callback + "\" run...");
					callback.run();
				}
			}
		};
	}
}
