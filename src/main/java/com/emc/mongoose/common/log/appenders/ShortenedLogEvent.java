package com.emc.mongoose.common.log.appenders;

import org.apache.logging.log4j.core.LogEvent;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created on 05.05.16.
 */
public class ShortenedLogEvent implements Serializable {

	private final String level;
	private final String loggerName;
	private final String threadName;
	private final long timeStamp;
	private final String message;

	public ShortenedLogEvent(LogEvent logEvent) {
		level = logEvent.getLevel().name();
		loggerName = logEvent.getLoggerName();
		threadName = logEvent.getThreadName();
		timeStamp = logEvent.getTimeMillis();
		message = logEvent.getMessage().getFormattedMessage();
	}

	public static class SleComparator implements Comparator<ShortenedLogEvent> {

		@Override
		public int compare(ShortenedLogEvent sle1, ShortenedLogEvent sle2) {
			return Long.compare(sle1.timeStamp, sle2.timeStamp);
		}

	}
}
