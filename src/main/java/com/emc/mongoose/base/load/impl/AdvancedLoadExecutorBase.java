package com.emc.mongoose.base.load.impl;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.AppendableDataItem;
import com.emc.mongoose.base.data.UpdatableDataItem;
import com.emc.mongoose.util.conf.RunTimeConfig;
//
import com.emc.mongoose.util.logging.Markers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.RemoteException;
import java.util.concurrent.ThreadLocalRandom;
/**
 Created by kurila on 15.12.14.
 */
public abstract class AdvancedLoadExecutorBase<T extends AppendableDataItem & UpdatableDataItem>
extends LoadExecutorBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final AsyncIOTask.Type loadType;
	private final int countUpdPerReq;
	//
	protected AdvancedLoadExecutorBase(
		final RunTimeConfig runTimeConfig, final RequestConfig<T> reqConfig, final String[] addrs,
		final int threadsPerNode, final String listFile, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias, final int countUpdPerReq
	) throws ClassCastException {
		super(
			runTimeConfig, reqConfig, addrs, threadsPerNode, listFile, maxCount,
			sizeMin, sizeMax, sizeBias
		);
		this.loadType = reqConfig.getLoadType();
		if(loadType == AsyncIOTask.Type.UPDATE) {
			if(countUpdPerReq < 0) {
				throw new IllegalArgumentException(
					String.format("Invalid updates per request count: %d", countUpdPerReq)
				);
			}
			this.countUpdPerReq = countUpdPerReq;
		} else {
			this.countUpdPerReq = -1;
		}
	}
	// intercepts the data items which should be scheduled for update or append
	@Override
	public final void submit(final T dataItem)
	throws InterruptedException, RemoteException {
		if(dataItem != null) {
			switch(loadType) {
				case UPDATE:
					dataItem.updateRandomRanges(countUpdPerReq);
					if(LOG.isTraceEnabled(Markers.MSG)) {
						LOG.trace(
							Markers.MSG, "Modified {} ranges for object \"{}\"",
							countUpdPerReq, dataItem
						);
					}
					break;
				case APPEND:
					final long appendSize = ThreadLocalRandom
						.current()
						.nextLong(sizeMin, sizeMax + 1);
					dataItem.append(appendSize);
					if(LOG.isTraceEnabled(Markers.MSG)) {
						LOG.trace(
							Markers.MSG, "Append the object \"{}\": +{}",
							dataItem, RunTimeConfig.formatSize(appendSize)
						);
					}
					break;
				default:
					break;
			}
		}
		super.submit(dataItem);
	}
}
