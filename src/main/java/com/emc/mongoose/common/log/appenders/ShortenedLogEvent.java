package com.emc.mongoose.common.log.appenders;

import org.apache.logging.log4j.core.LogEvent;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created on 05.05.16.
 */
public class ShortenedLogEvent implements Serializable {

	private String level;
	private String loggerName;
	private String threadName;
	private final long timeStamp;
	private String message;

	public ShortenedLogEvent(LogEvent logEvent) {
		level = logEvent.getLevel().name();
		loggerName = logEvent.getLoggerName();
		threadName = logEvent.getThreadName();
		timeStamp = logEvent.getTimeMillis();
		message = logEvent.getMessage().getFormattedMessage();
	}

	public ShortenedLogEvent(final long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public static class SleComparator implements Comparator<ShortenedLogEvent> {

		@Override
		public int compare(ShortenedLogEvent sle1, ShortenedLogEvent sle2) {
			return Long.compare(sle1.timeStamp, sle2.timeStamp);
		}
	}

	@Override
	public String toString() {
		return String.valueOf(timeStamp);
	}
}
