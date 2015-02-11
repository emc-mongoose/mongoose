package com.emc.mongoose.util.logging;
//
import org.apache.commons.lang.text.StrBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
/**
 Created by kurila on 17.09.14.
 The common way to handle the exceptions by logger.
 */
public final class TraceLogger {
	//
	public static void failure(
		final Logger logger, final Level level, final Throwable thrown, final String msg
	) {
		final StrBuilder msgBuilder = new StrBuilder();
		synchronized(logger) {
			logger.log(
				level, Markers.ERR,
				String.format("%s: %s", msg, thrown == null ? null : thrown.toString())
			);
			for(Throwable cause = thrown; cause != null; cause = cause.getCause()) {
				msgBuilder.append("\n\t").append(cause.toString());
				if(!logger.isTraceEnabled(Markers.ERR)) {
					break;
				}
				for(final StackTraceElement ste : thrown.getStackTrace()) {
					msgBuilder.append("\n\t\t").append(ste.toString());
				}
			}
			if(msgBuilder.size() > 0) {
				logger.log(Level.TRACE, Markers.ERR, msgBuilder.toString());
			}
		}
	}
	//
	public static void trace(
		final Logger logger, final Level level, final Marker marker, final String msg
	) {
		final StrBuilder msgBuilder = new StrBuilder(msg);
		final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		if(stackTrace.length > 2) {
			for(int i = 2; i < stackTrace.length; i ++) {
				msgBuilder.append("\n\t").append(stackTrace[i]);
			}
		}
		logger.log(level, marker, msgBuilder.toString());
	}
}
