package com.emc.mongoose.common.concurrent;

import com.emc.mongoose.common.io.Output;
import static com.emc.mongoose.common.Constants.BATCH_SIZE;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 Created by kurila on 21.02.17.
 */
public final class BatchQueueOutputTask<T>
extends ArrayList
implements Runnable {

	private final BlockingQueue<T> queue;
	private final Output<T> output;
	private final Set<Runnable> svcTasks;
	private final Lock lock = new ReentrantLock();

	private int n, m;
	
	public BatchQueueOutputTask(
		final BlockingQueue<T> queue, final Output<T> output, final Set<Runnable> svcTasks
	) {
		this.queue = queue;
		this.output = output;
		this.svcTasks = svcTasks;
	}
	
	@Override
	public final void run() {
		if(lock.tryLock()) {
			try {
				n = size();
				if(n < BATCH_SIZE) {
					n += queue.drainTo(this, BATCH_SIZE - n);
				}

				if(n > 0) {
					m = output.put(this, 0, n);
					removeRange(0, m);
				}
			} catch(final EOFException e) {
				svcTasks.remove(this);
			} catch(final Throwable t) {
				t.printStackTrace(System.err);
			} finally {
				lock.unlock();
			}
		}
	}
}
