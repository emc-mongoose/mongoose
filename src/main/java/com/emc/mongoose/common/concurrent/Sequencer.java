package com.emc.mongoose.common.concurrent;
//
import com.emc.mongoose.common.log.Markers;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
/**
 Created by andrey on 04.08.15.
 */
public class Sequencer
extends Thread {
	//
	private final static Logger LOG = LogManager.getLogger();
	public final static int DEFAULT_TASK_QUEUE_SIZE = 0x1000;
	//
	private final Queue<Runnable> queue;
	private final AtomicLong queueSize = new AtomicLong(0);
	//
	public Sequencer(final String name, boolean daemonFlag) {
		super(name);
		setDaemon(daemonFlag);
		queue = new ConcurrentLinkedQueue<>();
	}
	//
	public final <V> Future<V> submit(final RunnableFuture<V> task)
	throws InterruptedException {
		while(queueSize.get() > DEFAULT_TASK_QUEUE_SIZE) {
			LockSupport.parkNanos(1); Thread.yield();
		}
		queueSize.incrementAndGet();
		queue.add(task);
		return task;
	}
	//
	@Override
	public final void run() {
		try {
			Runnable nextTask;
			while(!isInterrupted()) {
				nextTask = queue.poll();
				if(nextTask == null) {
					LockSupport.parkNanos(1);
					nextTask = queue.poll();
					if(nextTask == null) {
						Thread.yield();
					} else {
						nextTask.run();
					}
				} else {
					nextTask.run();
				}
			}
		} finally {
			LOG.debug(Markers.MSG, "{}: finished", getName());
			queue.clear();
		}
	}
}
