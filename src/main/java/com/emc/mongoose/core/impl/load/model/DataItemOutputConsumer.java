package com.emc.mongoose.core.impl.load.model;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemDst;
import com.emc.mongoose.core.api.load.model.Consumer;
//
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
/**
 Created by kurila on 19.06.15.
 */
public class DataItemOutputConsumer<T extends DataItem>
extends AsyncDataItemDstBase<T>
implements Consumer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final DataItemDst<T> itemOut;
	//
	public DataItemOutputConsumer(final DataItemDst<T> itemOut, final long maxCount) {
		super(
			maxCount > 0 ? maxCount : Long.MAX_VALUE,
			RunTimeConfig.getContext().getTasksMaxQueueSize(),
			RunTimeConfig.getContext().isShuffleItemsEnabled(),
			RunTimeConfig.getContext().getBatchSize()
		);
		setName("consume" + (maxCount > 0 ? maxCount : "") + "<" + itemOut + ">");
		this.itemOut = itemOut;
	}
	//
	@Override
	protected void feedSeq(final T dataItem)
	throws InterruptedException, RemoteException {
		try {
			itemOut.put(dataItem);
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to put the data item");
		}
	}
	//
	@Override
	protected int feedSeqBatch(final List<T> dataItems, final int from, final int to)
	throws InterruptedException, RemoteException {
		int n = 0;
		try {
			n = itemOut.put(dataItems, from, to);
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to put the data items");
		}
		return n;
	}
	//
	@Override
	public void close()
	throws IOException {
		shutdown();
		LOG.debug(
			Markers.MSG, "{}: stopped consuming, count is {}", getName(), counterPreSubm.get()
		);
		try {
			join();
			LOG.debug(
				Markers.MSG, "{}: waiting for the queue remaining content processing is done",
				getName()
			);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Unexpected interruption while closing");
		}
		// close
		try {
			itemOut.close();
		} finally {
			super.close();
		}
	}
}
