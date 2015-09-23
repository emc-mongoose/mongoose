package com.emc.mongoose.server.impl.load.model;
//
//import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.impl.data.model.ItemBlockingQueue;
import com.emc.mongoose.server.api.load.model.RemoteItemBuffDst;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
/**
 Created by kurila on 25.06.14.
 A log consumer which accumulates the data items until the accumulated data is externally taken.
 */
public final class BasicItemBuffDst<T extends DataItem>
extends ItemBlockingQueue<T>
implements RemoteItemBuffDst<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final int buffSize;
	//
	public BasicItemBuffDst(final int maxQueueSize, final int buffSize) {
		super(new ArrayBlockingQueue<T>(maxQueueSize));
		this.buffSize = buffSize;
	}
	//
	//
	@Override
	public final List<T> fetchItems() {
		final List<T> buff = new ArrayList<>(buffSize);
		final int n = queue.drainTo(buff, buffSize);
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, "Sending next {} items to the client", n);
		}
		return buff;
	}
	//
	@Override
	public void close() {
		queue.clear();
	}
}
