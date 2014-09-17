package com.emc.mongoose.logging;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 17.09.14.
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
			final Throwable cause = thrown.getCause();
			if(cause==null) {
				logger.catching(Level.TRACE, thrown);
			} else {
				logger.trace(Markers.ERR, cause.toString(), cause.getCause());
			}
		}
	}
	//
}
