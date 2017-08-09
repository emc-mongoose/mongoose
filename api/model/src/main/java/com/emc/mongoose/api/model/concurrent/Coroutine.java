package com.emc.mongoose.api.model.concurrent;

import com.emc.mongoose.api.common.concurrent.StoppableTask;

/**
 * Created by kurila on 26.07.17.
 */
public interface Coroutine
extends StoppableTask {

	/**
	 The soft limit for the coroutine invocation duration.
	 The coroutine implementation should care about its own invocation duration.
	 */
	int TIMEOUT_NANOS = 100_000_000;
}
