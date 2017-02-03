package com.emc.mongoose.common.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
/**
 Created by andrey on 04.08.15.
 */
@Deprecated
public final class BlockingQueueTaskSequencer
extends Thread
implements TaskSequencer {

	@Deprecated
	public static final BlockingQueueTaskSequencer INSTANCE = new BlockingQueueTaskSequencer(
		"blockingQueueTaskSequencer", true, DEFAULT_TASK_QUEUE_SIZE_LIMIT
	);

	private final BlockingQueue<Runnable> queue;
	private final int batchSize;
	private final Collection<Runnable> buff;

	private BlockingQueueTaskSequencer(
		final String name, boolean daemonFlag, final int batchSize
	) {
		super(name);
		setDaemon(daemonFlag);
		queue = new ArrayBlockingQueue<>(DEFAULT_TASK_QUEUE_SIZE_LIMIT, false);
		this.batchSize = batchSize;
		buff = new ArrayList<>(batchSize);
	}

	@Deprecated @Override
	public final <V> Future<V> submit(final RunnableFuture<V> task) {
		try {
			queue.put(task);
			return task;
		} catch(final InterruptedException e) {
			return null;
		}
	}

	@Override
	public final void run() {
		int n;
		try {
			while(! isInterrupted()) {
				n = queue.drainTo(buff, batchSize);
				if(n > 0) {
					for(final Runnable nextTask : buff) {
						try {
							nextTask.run();
						} catch(final Exception e) {
							e.printStackTrace(System.err);
						}
					}
					buff.clear();
				} else {
					Thread.sleep(1);
				}
			}
		} catch(final InterruptedException ignore) {
		} finally {
			queue.clear();
		}
	}
}
