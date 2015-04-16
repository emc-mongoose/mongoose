package com.emc.mongoose.server.impl.load.model;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.core.impl.load.model.LogConsumer;
//
import com.emc.mongoose.server.api.load.model.RecordFrameBuffer;
//
import org.apache.commons.collections4.queue.CircularFifoQueue;
//
import java.rmi.RemoteException;
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
	private final static int LOCK_TIMEOUT_SEC = 10;
	//
	@SuppressWarnings("unchecked")
	private final CircularFifoQueue<T> buff = new CircularFifoQueue<>(0x100000);
	private final Lock submLock = new ReentrantLock();
	//
	public FrameBuffConsumer() {
		super(Long.MAX_VALUE, 1);
	}
	//
	@Override
	public final void submit(final T data)
	throws RejectedExecutionException, InterruptedException {
		if(submLock.tryLock(LOCK_TIMEOUT_SEC, TimeUnit.SECONDS)) {
			try {
				buff.add(data);
			} finally {
				submLock.unlock();
			}
		} else {
			throw new RejectedExecutionException("Failed to acquire the lock for submit method");
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final T[] takeFrame()
	throws RemoteException {
		final T[] frame;
		try {
			if(submLock.tryLock(LOCK_TIMEOUT_SEC, TimeUnit.SECONDS)) {
				frame = (T[]) new Object[buff.size()];
				buff.toArray(frame);
				buff.clear();
			} else {
				throw new RemoteException("Failed to acquire the lock for takeFrame method");
			}
		} catch(final InterruptedException e) {
			throw new RemoteException("Interrupted", e);
		} finally {
			submLock.unlock();
		}
		return frame;
	}
	//
}
