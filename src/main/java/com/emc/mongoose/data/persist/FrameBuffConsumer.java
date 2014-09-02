package com.emc.mongoose.data.persist;
//
import com.emc.mongoose.data.UniformData;
import com.emc.mongoose.data.UniformDataSource;
import com.emc.mongoose.remote.RecordFrameBuffer;
//
import org.apache.http.annotation.ThreadSafe;
//
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/**
 Created by kurila on 25.06.14.
 */
@ThreadSafe
public final class FrameBuffConsumer<T extends UniformData>
extends LogConsumer<T>
implements RecordFrameBuffer<T> {
	//
	private final ConcurrentLinkedQueue<T> buff = new ConcurrentLinkedQueue<>();
	private final Lock submLock = new ReentrantLock();
	private final UniformDataSource dataSrc;
	//
	public FrameBuffConsumer(UniformDataSource dataSrc) {
		super(dataSrc);
		this.dataSrc = dataSrc;
	}
	//
	@Override
	public final void submit(final T data) {
		if(data!=null) {
			submLock.lock();
			buff.add(data);
			submLock.unlock();
		}
	}
	//
	@Override
	public final UniformDataSource getDataSource() {
		return dataSrc;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final ArrayList<T> takeFrame() {
		submLock.lock();
		final ArrayList<T> frame = new ArrayList<>(buff.size());
		frame.addAll(buff);
		buff.clear();
		submLock.unlock();
		return frame;
	}
	//
}
