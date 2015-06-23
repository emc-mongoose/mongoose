package com.emc.mongoose.server.impl.load.model;
//
//import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.core.impl.load.model.AsyncConsumerBase;
import com.emc.mongoose.server.api.load.model.RecordFrameBuffer;
import org.apache.commons.collections4.queue.CircularFifoQueue;
//
//import org.apache.log.log4j.LogManager;
//import org.apache.log.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
/**
 Created by kurila on 25.06.14.
 A log consumer which accumulates the data items until the accumulated data is externally taken.
 */
public final class FrameBuffConsumer<T extends DataItem>
extends AsyncConsumerBase<T>
implements RecordFrameBuffer<T> {
	//
	//private final static Logger LOG = LogManager.getLogger();
	private final static int BUFF_SIZE = 0x100000;
	private final AtomicReference<CircularFifoQueue<T>> buff = new AtomicReference<>(
		new CircularFifoQueue<T>(BUFF_SIZE)
	);
	//
	public FrameBuffConsumer(
		final Class<T> dataCls, final RunTimeConfig runTimeConfig, final long maxCount
	) {
		super(
			maxCount, runTimeConfig.getRunRequestQueueSize(),
			runTimeConfig.getRunSubmitTimeOutMilliSec()
		);
		setName(Thread.currentThread().getName() + "-" + getClass().getSimpleName());
		start();
	}
	//
	@Override
	protected final void submitSync(final T dataItem)
	throws InterruptedException, RemoteException {
		buff.get().add(dataItem);
	}
	//
	@Override
	public final Collection<T> takeFrame() {
		return buff.getAndSet(new CircularFifoQueue<T>(BUFF_SIZE));
	}
}
