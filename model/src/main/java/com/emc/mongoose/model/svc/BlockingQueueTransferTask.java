package com.emc.mongoose.model.svc;

import com.emc.mongoose.common.collection.OptLockArrayBuffer;
import com.emc.mongoose.common.collection.OptLockBuffer;
import com.emc.mongoose.common.io.Output;
import static com.emc.mongoose.common.Constants.BATCH_SIZE;

import java.io.EOFException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 Created by kurila on 21.02.17.
 */
public final class BlockingQueueTransferTask<T>
implements Runnable {
	
	private final BlockingQueue<T> queue;
	private final Output<T> output;
	private final List<Runnable> svcTasks;
	private final OptLockBuffer<T> buff;

	private int n, m;
	
	public BlockingQueueTransferTask(
		final BlockingQueue<T> queue, final Output<T> output, final List<Runnable> svcTasks
	) {
		this.queue = queue;
		this.output = output;
		this.svcTasks = svcTasks;
		this.buff = new OptLockArrayBuffer<>(BATCH_SIZE);
	}
	
	@Override
	public final void run() {
		if(buff.tryLock()) {
			try {
				n = buff.size();
				if(n < BATCH_SIZE) {
					n += queue.drainTo(buff, BATCH_SIZE - n);
				}
				if(n > 0) {
					m = output.put(buff, 0, n);
					buff.removeRange(0, m);
				}
			} catch(final EOFException e) {
				svcTasks.remove(this);
			} catch(final Throwable t) {
				t.printStackTrace(System.err);
			} finally {
				buff.unlock();
			}
		}
	}
}
