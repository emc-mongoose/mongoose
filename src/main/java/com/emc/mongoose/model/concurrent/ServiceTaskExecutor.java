package com.emc.mongoose.model.concurrent;

import com.github.akurilov.concurrent.coroutine.CoroutinesExecutor;

public interface ServiceTaskExecutor {

	CoroutinesExecutor INSTANCE = new CoroutinesExecutor();

	static void setThreadCount(final int threadCount) {
		INSTANCE.setThreadCount(threadCount);
	}
}
