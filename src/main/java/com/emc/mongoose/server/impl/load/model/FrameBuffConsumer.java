package com.emc.mongoose.server.impl.load.model;
//
//import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.core.impl.load.model.LogConsumer;
//
import com.emc.mongoose.server.api.load.model.RecordFrameBuffer;
//
import org.apache.commons.collections4.queue.CircularFifoQueue;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
import java.lang.reflect.Array;
import java.rmi.RemoteException;
import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/**
 Created by kurila on 25.06.14.
 A logging consumer which accumulates the data items until the accumulated data is externally taken.
 */
public final class FrameBuffConsumer<T extends DataItem>
extends LogConsumer<T>
implements RecordFrameBuffer<T> {
	//
	//private final static Logger LOG = LogManager.getLogger();
	private final static int LOCK_TIMEOUT_SEC = 10;
	//
	@SuppressWarnings("unchecked")
	private final Queue<T> buff = new CircularFifoQueue<>(0x100000);
	private final Lock buffLock = new ReentrantLock();
	//
	public FrameBuffConsumer() {
		super(Long.MAX_VALUE, 1);
	}
	//
	@Override
	public final void submit(final T data)
	throws RejectedExecutionException, InterruptedException {
		if(buffLock.tryLock(LOCK_TIMEOUT_SEC, TimeUnit.SECONDS)) {
			try {
				buff.add(data);
			} finally {
				buffLock.unlock();
			}
		} else {
			throw new RejectedExecutionException("Failed to acquire the lock for submit method");
		}
	}
	//
	private T anyItem = null;
	//
	@Override @SuppressWarnings("unchecked")
	public final T[] takeFrame()
	throws RemoteException, InterruptedException {
		T frame[] = null;
		if(anyItem == null) {
			anyItem = buff.peek();
		}
		if(anyItem != null) {
			if(buffLock.tryLock(LOCK_TIMEOUT_SEC, TimeUnit.SECONDS)) {
				try {
					frame = (T[]) Array.newInstance(anyItem.getClass(), buff.size());
					buff.toArray(frame);
					buff.clear();
				//} catch(final Throwable t) {
				//	LogUtil.failure(LOG, Level.ERROR, t, "Failed");
				} finally {
					buffLock.unlock();
				}
			} else {
				throw new RemoteException("Failed to acquire the lock for takeFrame method");
			}
		}
		return frame;
	}
	//
}
