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
public final class BlockingQueueTaskSequencer
extends Thread
implements TaskSequencer {

	public final static int DEFAULT_TASK_QUEUE_SIZE = 0x1000;
	public final static BlockingQueueTaskSequencer INSTANCE = new BlockingQueueTaskSequencer(
		"blockingQueueTaskSequencer", true, DEFAULT_TASK_QUEUE_SIZE
	);
	static {
		INSTANCE.start();
	}

	private final BlockingQueue<Runnable> queue;
	private final int batchSize;
	private final Collection<Runnable> buff;

	protected BlockingQueueTaskSequencer(final String name, boolean daemonFlag, final int batchSize) {
		super(name);
		setDaemon(daemonFlag);
		queue = new ArrayBlockingQueue<>(DEFAULT_TASK_QUEUE_SIZE, false);
		this.batchSize = batchSize;
		buff = new ArrayList<>(batchSize);
	}

	@Override
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
			while(!isInterrupted()) {
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
					Thread.yield();
				}
			}
		} finally {
			queue.clear();
		}
	}
}
