package com.emc.mongoose.base.logging;

import static com.emc.mongoose.base.Constants.KEY_HOME_DIR;
import static com.emc.mongoose.base.Constants.KEY_STEP_ID;
import static com.emc.mongoose.base.Constants.LOCALE_DEFAULT;
import static com.emc.mongoose.base.Exceptions.throwUncheckedIfInterrupted;
import static com.emc.mongoose.base.env.DateUtil.TZ_UTC;
import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;

import com.emc.mongoose.base.concurrent.DaemonBase;
import java.util.Calendar;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.util.datetime.DatePrinter;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;

/** Created by kurila on 06.05.14. */
public interface LogUtil {

	DatePrinter FMT_DT = FastDateFormat.getInstance("yyyyMMdd.HHmmss.SSS", TZ_UTC, LOCALE_DEFAULT);

	// console colors
	String RED = "\u001B[31m",
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

	static String getDateTimeStamp() {
		return FMT_DT.format(Calendar.getInstance(TZ_UTC, LOCALE_DEFAULT).getTime());
	}

	static void init(final String homeDir, final String initialStepId) {
		ThreadContext.put(KEY_HOME_DIR, homeDir);
		ThreadContext.put(KEY_STEP_ID, initialStepId);
		try {
			Runtime.getRuntime()
							.addShutdownHook(
											new Thread("logCtxShutDownHook") {
												@Override
												public final void run() {
													shutdown();
												}
											});
		} catch (final Exception e) {
			throw new AssertionError(e);
		}
	}

	static void flushAll() {
		final var logCtx = ((LoggerContext) LogManager.getContext());
		for (final var logger : logCtx.getLoggers()) {
			for (final var appender : logger.getAppenders().values()) {
				if (appender instanceof AbstractOutputStreamAppender) {
					((AbstractOutputStreamAppender) appender).getManager().flush();
				}
			}
		}
	}

	static void shutdown()  {
		try {
			DaemonBase.closeAll();
		} catch (final Throwable cause) {
			throwUncheckedIfInterrupted(cause);
			cause.printStackTrace(System.err);
		} finally {
			LogManager.shutdown();
		}
	}

	static String getFailureRatioAnsiColorCode(final long succ, final long fail) {
		if (fail == 0) {
			return "\u001B[38;2;0;200;0m";
		}
		if (fail >= succ) {
			return "\u001B[38;2;" + ((int) (200 + ((double) 55 * fail) / (succ + fail))) + ";0;0m";
		}
		return "\u001B[38;2;"
						+
						/* R */ ((int) (400 * Math.sqrt(((double) fail) / (succ + fail))))
						+ ";"
						+
						/* G */ ((int) (((double) 200 * succ / (succ + fail))))
						+ ";"
						+
						/* B */ "0m";
	}

	ThreadLocal<StringBuilder> THR_LOC_MSG_BUILDER = ThreadLocal.withInitial(StringBuilder::new);

	static void exception(
					final Level level, final Throwable e, final String msgPattern, final Object... args) {
		if (Loggers.ERR.isTraceEnabled()) {
			trace(Loggers.ERR, level, e, msgPattern, args);
		} else {
			final var msgBuilder = THR_LOC_MSG_BUILDER.get();
			msgBuilder.setLength(0);
			msgBuilder.append(msgPattern).append("\n\tCAUSE: ").append(e);
			for (var cause = e.getCause(); cause != null; cause = cause.getCause()) {
				msgBuilder.append("\n\tCAUSE: ").append(cause.toString());
			}
			final var msg = Loggers.ERR.getMessageFactory().newMessage(msgBuilder.toString(), args);
			Loggers.ERR.log(level, msg);
		}
	}

	static void trace(
					final Logger logger,
					final Level level,
					final Throwable e,
					final String msgPattern,
					final Object... args) {
		logger.log(level, logger.getMessageFactory().newMessage(msgPattern + ": " + e, args), e);
	}
}
