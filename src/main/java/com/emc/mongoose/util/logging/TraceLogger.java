package com.emc.mongoose.util.logging;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
/**
 Created by kurila on 17.09.14.
 The common way to handle the exceptions by logger.
 */
public final class TraceLogger {
	//
	private static final String FMT_MSG = "%s: %s";
	//
	public static void failure(
		final Logger logger, final Level level, final Throwable thrown, final String msg
	) {
		final StringBuilder msgBuilder = new StringBuilder(
			String.format(FMT_MSG, msg, thrown.toString())
		);
		if(logger.isTraceEnabled(Markers.ERR)) {
			Throwable cause = thrown.getCause();
			while(cause != null) {
				msgBuilder.append("\n\t").append(cause.toString());
				for(final StackTraceElement ste : thrown.getStackTrace()) {
					msgBuilder.append("\n\t\t").append(ste.toString());
				}
				cause = cause.getCause();
			}
			logger.log(Level.TRACE, Markers.ERR, msgBuilder.toString());
		}
	}
	//
	public static void trace(
		final Logger logger, final Level level, final Marker marker, final String msg
	) {
		final StringBuilder msgBuilder = new StringBuilder(msg);
		final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		for(final StackTraceElement ste : stackTrace) {
			msgBuilder.append("\n\t").append(ste.toString());
		}
		logger.log(level, marker, msgBuilder.toString());
	}
}
