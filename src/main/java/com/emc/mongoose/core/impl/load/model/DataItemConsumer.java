package com.emc.mongoose.core.impl.load.model;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemDst;
//
//
import com.emc.mongoose.core.api.data.model.DataItemSrc;
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
public class DataItemConsumer<T extends DataItem>
extends AsyncDataItemConsumerBase<T>
implements DataItemDst<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final DataItemDst<T> itemDst;
	//
	public DataItemConsumer(final DataItemDst<T> itemDst, final long maxCount) {
		super(
			maxCount > 0 ? maxCount : Long.MAX_VALUE,
			RunTimeConfig.getContext().getTasksMaxQueueSize(),
			RunTimeConfig.getContext().isShuffleItemsEnabled(),
			RunTimeConfig.getContext().getBatchSize()
		);
		setName("consume" + (maxCount > 0 ? maxCount : "") + "<" + itemDst + ">");
		this.itemDst = itemDst;
	}
	//
	@Override
	protected int feedSeq(final List<T> dataItems, final int from, final int to)
	throws InterruptedException, RemoteException {
		int n = 0;
		try {
			n = itemDst.put(dataItems, from, to);
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to put the data items");
		}
		return n;
	}
	//
	@Override
	protected void closeActually()
	throws IOException {
		try {
			itemDst.close();
		} finally {
			super.closeActually();
		}
	}
	//
	@Override
	public DataItemSrc<T> getDataItemSrc()
	throws IOException {
		return itemDst.getDataItemSrc();
	}
}
