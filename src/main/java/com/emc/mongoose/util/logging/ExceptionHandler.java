package com.emc.mongoose.util.logging;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 17.09.14.
 The common way to handle the exceptions by logger.
 */
public final class ExceptionHandler {
	//
	private static final String FMT_MSG = "%s: %s";
	//
	public static void trace(
		final Logger logger, final Level level, final Throwable thrown, final String msg
	) {
		logger.log(level, Markers.ERR, String.format(FMT_MSG, msg, thrown.toString()));
		if(logger.isTraceEnabled(Markers.ERR)) {
			Throwable cause = thrown;
			final StringBuilder msgBuilder = new StringBuilder(thrown.toString());
			do {
				msgBuilder.append('\n').append(cause.toString());
				for(final StackTraceElement stackTraceElement: thrown.getStackTrace()) {
					msgBuilder.append("\n\t").append(stackTraceElement.toString());
				}
				cause = cause.getCause();
			} while(cause != null);
			logger.log(Level.TRACE, Markers.ERR, msgBuilder.toString());
		}
	}
}
