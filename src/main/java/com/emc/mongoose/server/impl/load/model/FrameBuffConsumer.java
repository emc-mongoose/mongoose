package com.emc.mongoose.server.impl.load.model;
//
//import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.impl.load.model.AsyncConsumerBase;
//
import com.emc.mongoose.server.api.load.model.RecordFrameBuffer;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
/**
 Created by kurila on 25.06.14.
 A log consumer which accumulates the data items until the accumulated data is externally taken.
 */
public final class FrameBuffConsumer<T>
extends AsyncConsumerBase<T>
implements RecordFrameBuffer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final int buffSize;
	//
	public FrameBuffConsumer(
		final Class<T> dataCls, final RunTimeConfig rtConfig, final long maxCount
	) {
		super(
			maxCount, rtConfig.getTasksMaxQueueSize(),
			rtConfig.isShuffleItemsEnabled(), rtConfig.getBatchSize()
		);
		buffSize = rtConfig.getBatchSize();
		setName(Thread.currentThread().getName() + "-" + getClass().getSimpleName());
		start();
	}
	//
	@Override
	protected final void startActually() {
		// do not start the consuming thread actually
	}
	//
	@Override
	public final void run() {
		// do not consume the queue
	}
	//
	@Override
	protected final void feedSeq(final T item) {
		// do nothing
	}
	//
	@Override
	protected final void feedSeqBatch(final List<T> items) {
		// do nothing
	}
	//
	@Override
	public final Collection<T> takeFrame() {
		final Collection<T> buff = new ArrayList<>(buffSize);
		final int n = queue.drainTo(buff, buffSize);
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, "Sending next {} items to the client", n);
		}
		return buff;
	}
}
