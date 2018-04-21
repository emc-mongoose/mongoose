package com.emc.mongoose.api.model.concurrent;

import java.util.Map;

/**
 Created by andrey on 08.08.17.
 */
public final class ThreadDump {

	private final Map<Thread, StackTraceElement[]> threadDumpData = Thread.getAllStackTraces();

	@Override
	public final String toString() {
		final StringBuilder threadDumpStr = new StringBuilder();
		final String lineSep = System.lineSeparator();
		for(final Thread thread : threadDumpData.keySet()) {
			threadDumpStr
				.append(thread.getName())
				.append(" (state: ")
				.append(thread.getState())
				.append("):")
				.append(lineSep);
			final StackTraceElement[] threadStackTrace = threadDumpData.get(thread);
			for(final StackTraceElement ste : threadStackTrace) {
				threadDumpStr
					.append('\t')
					.append(ste)
					.append(lineSep);
			}
		}
		return threadDumpStr.toString();
	}
}
