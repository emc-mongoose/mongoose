package com.emc.mongoose.server.impl.load.model;
//
//import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.core.api.data.DataItem;
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
import java.util.concurrent.atomic.AtomicReference;
/**
 Created by kurila on 25.06.14.
 A logging consumer which accumulates the data items until the accumulated data is externally taken.
 */
public final class FrameBuffConsumer<T extends DataItem>
implements RecordFrameBuffer<T> {
	//
	//private final static Logger LOG = LogManager.getLogger();
	private final static int BUFF_SIZE = 0x100000;
	//
	private AtomicReference<Queue<T>> inBuffRef = new AtomicReference<>(
		(Queue<T>) new CircularFifoQueue<T>(BUFF_SIZE)
	);
	//
	@Override
	public final void submit(final T dataItem) {
		inBuffRef.get().add(dataItem);
	}
	//
	private T anyItem = null;
	//
	@Override @SuppressWarnings("unchecked")
	public final T[] takeFrame()
	throws InterruptedException, RemoteException {
		T frame[] = null;
		Queue<T> outBuff;
		if(anyItem == null) {
			anyItem = inBuffRef.get().peek();
		}
		if(anyItem != null) {
			outBuff = inBuffRef.getAndSet(new CircularFifoQueue<T>(BUFF_SIZE));
			frame = (T[]) Array.newInstance(anyItem.getClass(), outBuff.size());
			outBuff.toArray(frame);
			outBuff = null; // dispose
		}
		return frame;
	}
	//
	@Override
	public final void shutdown() {
	}
	//
	@Override
	public final void close() {
	}
	//
	@Override
	public final long getMaxCount() {
		return Long.MAX_VALUE;
	}
}
