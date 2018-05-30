package com.emc.mongoose.concurrent;

import com.github.akurilov.fiber4j.FibersExecutor;

public interface ServiceTaskExecutor {

	FibersExecutor INSTANCE = new FibersExecutor();

	static void setThreadCount(final int threadCount) {
		INSTANCE.setThreadCount(threadCount);
	}
}
