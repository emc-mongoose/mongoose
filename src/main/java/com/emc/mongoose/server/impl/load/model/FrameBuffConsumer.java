package com.emc.mongoose.server.impl.load.model;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.core.impl.load.model.LogConsumer;
//
import com.emc.mongoose.server.api.load.model.RecordFrameBuffer;
//
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
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
	private final ConcurrentLinkedQueue<T> buff = new ConcurrentLinkedQueue<>();
	private final Lock submLock = new ReentrantLock();
	//
	public FrameBuffConsumer() {
		super(Long.MAX_VALUE, 1);
	}
	//
	@Override
	public final void submit(final T data) {
		if(data!=null) {
			if(submLock.tryLock()) {
				try {
					buff.add(data);
				} finally {
					submLock.unlock();
				}
			}
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final ArrayList<T> takeFrame() {
		ArrayList<T> frame = null;
		if(submLock.tryLock()) {
			try {
				frame = new ArrayList<>(buff.size());
				frame.addAll(buff);
				buff.clear();
			} finally {
				submLock.unlock();
			}
		}
		return frame;
	}
	//
}
