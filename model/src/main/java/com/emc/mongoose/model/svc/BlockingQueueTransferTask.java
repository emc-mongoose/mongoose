package com.emc.mongoose.model.svc;

import com.emc.mongoose.common.collection.OptLockArrayBuffer;
import com.emc.mongoose.common.collection.OptLockBuffer;
import com.emc.mongoose.common.concurrent.SvcTask;
import com.emc.mongoose.common.concurrent.SvcTaskBase;
import com.emc.mongoose.common.io.Output;
import static com.emc.mongoose.common.Constants.BATCH_SIZE;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 Created by kurila on 21.02.17.
 */
public final class BlockingQueueTransferTask<T>
extends SvcTaskBase {
	
	private final BlockingQueue<T> queue;
	private final Output<T> output;
	private final OptLockBuffer<T> buff;

	private int n, m;

	public BlockingQueueTransferTask(
		final BlockingQueue<T> queue, final Output<T> output, final List<SvcTask> svcTasks
	) {
		super(svcTasks);
		this.queue = queue;
		this.output = output;
		this.buff = new OptLockArrayBuffer<>(BATCH_SIZE);
	}
	
	@Override
	protected final void invoke() {
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
		queue.clear();
		buff.clear();
	}
}
