package com.emc.mongoose.model.svc;

import com.emc.mongoose.common.collection.OptLockArrayBuffer;
import com.emc.mongoose.common.collection.OptLockBuffer;
import com.emc.mongoose.common.concurrent.SvcTask;
import com.emc.mongoose.common.concurrent.SvcTaskBase;
import com.emc.mongoose.common.io.Output;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 21.02.17.
 */
public final class BlockingQueueTransferTask<T>
extends SvcTaskBase {
	
	private final BlockingQueue<T> queue;
	private final Output<T> output;
	private final OptLockBuffer<T> buff;
	private final int batchSize;

	private int n, m;
	
	public BlockingQueueTransferTask(
		final BlockingQueue<T> queue, final Output<T> output, final int batchSize,
		final List<SvcTask> svcTasks
	) {
		super(svcTasks);
		this.queue = queue;
		this.output = output;
		this.batchSize = batchSize;
		this.buff = new OptLockArrayBuffer<>(batchSize);
	}
	
	@Override
	protected final void invoke() {
		if(buff.tryLock()) {
			try {
				n = buff.size();
				if(n < batchSize) {
					n += queue.drainTo(buff, batchSize - n);
				}
				if(n > 0) {
					if(n == 1) {
						if(output.put(buff.get(0))) {
							buff.clear();
						}
					} else {
						m = output.put(buff, 0, n);
						buff.removeRange(0, m);
					}
				}
			} catch(final EOFException e) {
				try {
					close();
				} catch(final IOException ee) {
					ee.printStackTrace(System.err);
				}
			} catch(final Throwable t) {
				t.printStackTrace(System.err);
			} finally {
				buff.unlock();
			}
		}
	}

	@Override
	protected final void doClose()
	throws IOException {
		try {
			buff.tryLock(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
			buff.clear();
			queue.clear();
		} catch(final InterruptedException e) {
			e.printStackTrace(System.err);
		}
	}
}
