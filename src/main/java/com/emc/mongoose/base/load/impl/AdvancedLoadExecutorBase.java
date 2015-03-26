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
//
import java.rmi.RemoteException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
/**
 Created by kurila on 15.12.14.
 */
public abstract class AdvancedLoadExecutorBase<T extends AppendableDataItem & UpdatableDataItem>
extends LoadExecutorBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final AsyncIOTask.Type loadType;
	private final int countUpdPerReq;
	private final long sizeMin, sizeRange;
	private final float sizeBias;
	//
	protected AdvancedLoadExecutorBase(
		final RunTimeConfig runTimeConfig, final RequestConfig<T> reqConfig, final String[] addrs,
		final int connCountPerNode, final String listFile, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias, final int countUpdPerReq
	) throws ClassCastException {
		super(
			runTimeConfig, reqConfig, addrs, connCountPerNode, listFile, maxCount,
			sizeMin, sizeMax, sizeBias
		);
		//
		this.loadType = reqConfig.getLoadType();
		//
		int buffSize;
		if(sizeMin < BUFF_SIZE_LO) {
			buffSize = BUFF_SIZE_LO;
		} else if(sizeMin > BUFF_SIZE_HI) {
			buffSize = BUFF_SIZE_HI;
		} else {
			buffSize = (int) sizeMin;
		}
		LOG.debug(
			Markers.MSG, "Determined buffer size of {} for \"{}\"",
			RunTimeConfig.formatSize(buffSize), getName()
		);
		this.reqConfig.setBuffSize(buffSize);
		//
		switch(loadType) {
			case APPEND:
			case CREATE:
				if(sizeMin < 1) {
					throw new IllegalArgumentException(
						String.format(
							"Min data item size (%s) is less than 1 [bytes]",
							RunTimeConfig.formatSize(sizeMin)
						)
					);
				}
				if(sizeMin > sizeMax) {
					throw new IllegalArgumentException(
						String.format(
							"Min object size (%s) should be less than max (%s)",
							RunTimeConfig.formatSize(sizeMin), RunTimeConfig.formatSize(sizeMax)
						)
					);
				}
				if(sizeBias < 0) {
					throw new IllegalArgumentException(
						String.format("Object size bias (%f) should not be less than 0", sizeBias)
					);
				}
				break;
			case UPDATE:
				if(countUpdPerReq < 0) {
					throw new IllegalArgumentException(
						String.format("Invalid updates per request count: %d", countUpdPerReq)
					);
				}
				break;
		}
		//
		this.sizeMin = sizeMin;
		sizeRange = sizeMax - sizeMin;
		this.sizeBias = sizeBias;
		this.countUpdPerReq = countUpdPerReq;
	}
	// intercepts the data items which should be scheduled for update or append
	@Override
	public final void submit(final T dataItem)
	throws InterruptedException, RemoteException, RejectedExecutionException {
		if(dataItem != null) {
			switch(loadType) {
				case APPEND:
					final long nextSize = sizeMin +
						(long) (
							Math.pow(ThreadLocalRandom.current().nextDouble(), sizeBias) *
							sizeRange
						);
					dataItem.append(nextSize);
					if(LOG.isTraceEnabled(Markers.MSG)) {
						LOG.trace(
							Markers.MSG, "Append the object \"{}\": +{}",
							dataItem, RunTimeConfig.formatSize(nextSize)
						);
					}
					break;
				case UPDATE:
					dataItem.updateRandomRanges(countUpdPerReq);
					if(LOG.isTraceEnabled(Markers.MSG)) {
						LOG.trace(
							Markers.MSG, "Modified {} ranges for object \"{}\"",
							countUpdPerReq, dataItem
						);
					}
					break;
			}
		}
		super.submit(dataItem);
	}
}
