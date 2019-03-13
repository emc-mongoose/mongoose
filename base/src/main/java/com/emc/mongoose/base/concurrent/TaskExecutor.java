package com.emc.mongoose.base.concurrent;

import java.util.concurrent.Executor;

public interface TaskExecutor extends Executor, AutoCloseable {

	@Override
	void close();
}
