package com.emc.mongoose.common.concurrent;

import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 Created by kurila on 13.07.16.
 */
public class ConcurrentQueueTaskSequencer
extends Thread
implements TaskSequencer {

	private final static Logger LOG = LogManager.getLogger();
	public final static ConcurrentQueueTaskSequencer INSTANCE = new ConcurrentQueueTaskSequencer(
		"concurrentQueueTaskSequencer", true
	);
	static {
		INSTANCE.start();
	}

	private final Queue<Runnable> queue;
	private final AtomicInteger queueSize = new AtomicInteger(0);

	protected ConcurrentQueueTaskSequencer(final String name, boolean daemonFlag) {
		super(name);
		setDaemon(daemonFlag);
		queue = new ConcurrentLinkedQueue<>();
	}

	@Override
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
						LogUtil.exception(LOG, Level.WARN, e, "Task \"{}\" failed", nextTask);
					}
				} else {
					Thread.yield();
				}
			}
		} finally {
			LOG.debug(Markers.MSG, "{}: finished", getName());
			queue.clear();
		}
	}
}
