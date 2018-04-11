package com.emc.mongoose.api.model.concurrent;

import java.util.Map;

/**
 Created by andrey on 08.08.17.
 */
public final class ThreadDump {

	private final Map<Thread, StackTraceElement[]> threadDumpData = Thread.getAllStackTraces();

	@Override
	public final String toString() {
		final var threadDumpStr = new StringBuilder();
		final var lineSep = System.lineSeparator();
		for(final var thread : threadDumpData.keySet()) {
			threadDumpStr
				.append(thread.getName())
				.append(" (state: ")
				.append(thread.getState())
				.append("):")
				.append(lineSep);
			final var threadStackTrace = threadDumpData.get(thread);
			for(final var ste : threadStackTrace) {
				threadDumpStr
					.append('\t')
					.append(ste)
					.append(lineSep);
			}
		}
		return threadDumpStr.toString();
	}
}
