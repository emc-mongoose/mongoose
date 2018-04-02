package com.emc.mongoose.api.model.concurrent;

import com.github.akurilov.coroutines.CoroutinesProcessor;

public interface ServiceTaskExecutor {

	CoroutinesProcessor INSTANCE = new CoroutinesProcessor();

	static void setThreadCount(final int threadCount) {
		INSTANCE.setThreadCount(threadCount);
	}
}
