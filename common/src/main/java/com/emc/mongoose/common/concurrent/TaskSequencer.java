package com.emc.mongoose.common.concurrent;

import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;

/**
 Created by kurila on 13.07.16.
 */
public interface TaskSequencer
extends Runnable {

	int DEFAULT_TASK_QUEUE_SIZE_LIMIT = 0x1000;

	<V> Future<V> submit(final RunnableFuture<V> task)
	throws InterruptedException;

}
