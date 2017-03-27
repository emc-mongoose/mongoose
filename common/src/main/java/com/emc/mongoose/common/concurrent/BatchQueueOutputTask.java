package com.emc.mongoose.common.concurrent;

import com.emc.mongoose.common.io.Output;
import static com.emc.mongoose.common.Constants.BATCH_SIZE;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.LockSupport;

/**
 Created by kurila on 21.02.17.
 */
public final class BatchQueueOutputTask<T>
extends ArrayList<T>
implements Runnable {
	
	private final transient BlockingQueue<T> queue;
	private final transient Output<T> output;
	
	public BatchQueueOutputTask(final BlockingQueue<T> queue, final Output<T> output) {
		super(BATCH_SIZE);
		this.queue = queue;
		this.output = output;
	}
	
	@Override
	public final void run() {
		int n, m;
		final Thread currThread = Thread.currentThread();
		while(!currThread.isInterrupted()) {
			n = size();
			if(n == BATCH_SIZE) {
				LockSupport.parkNanos(1);
			} else {
				n = queue.drainTo(this, BATCH_SIZE - n);
			}
			if(n == 0) {
				LockSupport.parkNanos(1);
			} else {
				try {
					m = output.put(this, 0, n);
					removeRange(0, m);
				} catch(final EOFException e) {
					break;
				} catch(final IOException e) {
					e.printStackTrace(System.err);
				}
			}
		}
	}
}
