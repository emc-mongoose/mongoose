package com.emc.mongoose.common.concurrent;
//
import com.emc.mongoose.common.log.LogUtil;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.locks.LockSupport;
/**
 Created by andrey on 04.08.15.
 */
public class Sequencer
extends Thread {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final BlockingQueue<Runnable> queue;
	private final int batchSize;
	private final Collection<Runnable> buff;
	//
	public Sequencer(
		final String name, boolean daemonFlag, final int queueCapacity, final int batchSize
	) {
		super(name);
		setDaemon(daemonFlag);
		queue = new ArrayBlockingQueue<>(queueCapacity, false);
		this.batchSize = batchSize;
		buff = new ArrayList<>(batchSize);
	}
	//
	public final <V> Future<V> submit(final RunnableFuture<V> task)
	throws InterruptedException {
		queue.put(task);
		return task;
	}
	//
	@Override
	public final void run() {
		int n;
		try {
			while(true) {
				n = queue.drainTo(buff, batchSize);
				if(n > 0) {
					for(final Runnable nextTask : buff) {
						try {
							nextTask.run();
						} catch(final Exception e) {
							LogUtil.exception(
								LOG, Level.WARN, e, "Task \"{}\" failed", nextTask
							);
						}
					}
					buff.clear();
				} else {
					LockSupport.parkNanos(1);
				}
			}
		} finally {
			queue.clear();
		}
	}
}
