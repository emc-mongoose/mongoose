package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.io.Output;
import static com.emc.mongoose.common.Constants.BATCH_SIZE;
import com.emc.mongoose.ui.log.LogUtil;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	
	private static final Logger LOG = LogManager.getLogger();
	
	private final BlockingQueue<T> queue;
	private final Output<T> output;
	
	public BatchQueueOutputTask(final BlockingQueue<T> queue, final Output<T> output) {
		super(BATCH_SIZE);
		this.queue = queue;
		this.output = output;
	}
	
	@Override
	public final void run() {
		int n, m;
		while(true) {
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
					LogUtil.exception(LOG, Level.WARN, e, "Output failure");
				}
			}
		}
	}
}
