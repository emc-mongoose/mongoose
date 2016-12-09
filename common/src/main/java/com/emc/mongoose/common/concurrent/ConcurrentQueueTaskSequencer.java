package com.emc.mongoose.common.concurrent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 Created by kurila on 13.07.16.
 */
@Deprecated
public final class ConcurrentQueueTaskSequencer
extends Thread
implements TaskSequencer {

	@Deprecated
	public static final ConcurrentQueueTaskSequencer INSTANCE = new ConcurrentQueueTaskSequencer(
		"concurrentQueueTaskSequencer", true
	);

	private final Queue<Runnable> queue;
	private final AtomicInteger queueSize = new AtomicInteger(0);

	private ConcurrentQueueTaskSequencer(final String name, boolean daemonFlag) {
		super(name);
		setDaemon(daemonFlag);
		queue = new ConcurrentLinkedQueue<>();
	}

	@Deprecated @Override
	public final <V> Future<V> submit(final RunnableFuture<V> task) {
		while(true) {
			if(queueSize.get() < DEFAULT_TASK_QUEUE_SIZE_LIMIT) {
				if(queue.add(task)) {
					return task;
				}
			}
			Thread.yield();
		}
	}

	@Override
	public final void run() {
		Runnable nextTask;
		try {
			while(!isInterrupted()) {
				nextTask = queue.poll();
				if(nextTask != null) {
					try {
						nextTask.run();
					} catch(final Exception e) {
						e.printStackTrace(System.err);
					}
				} else {
					Thread.yield();
				}
			}
		} finally {
			queue.clear();
		}
	}
}
