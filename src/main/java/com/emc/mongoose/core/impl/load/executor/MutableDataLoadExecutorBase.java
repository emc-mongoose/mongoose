package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.common.conf.Constants;
// mongoose-common.jar
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.MutableDataItem;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.data.model.FileDataItemSrc;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.req.RequestConfig;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
/**
 Created by kurila on 15.12.14.
 */
public abstract class MutableDataLoadExecutorBase<T extends MutableDataItem>
extends LimitedRateLoadExecutorBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final IOTask.Type loadType;
	private final int countUpdPerReq;
	private final long sizeMin, sizeRange;
	private final float sizeBias;
	//
	protected MutableDataLoadExecutorBase(
		final RunTimeConfig runTimeConfig, final RequestConfig<T> reqConfig, final String[] addrs,
		final int connCountPerNode, final int threadCount,
		final ItemSrc<T> itemSrc, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias,
		final int manualTaskSleepMicroSecs, final float rateLimit, final int countUpdPerReq
	) throws ClassCastException {
		super(
			runTimeConfig, reqConfig, addrs, connCountPerNode, threadCount,
			itemSrc, maxCount, manualTaskSleepMicroSecs, rateLimit
		);
		//
		this.loadType = reqConfig.getLoadType();
		//
		int buffSize;
		if(itemSrc instanceof FileDataItemSrc) {
			final long approxDataItemSize = ((FileDataItemSrc) itemSrc).getApproxDataItemsSize(
				runTimeConfig.getBatchSize()
			);
			if(approxDataItemSize < Constants.BUFF_SIZE_LO) {
				buffSize = Constants.BUFF_SIZE_LO;
			} else if(approxDataItemSize > Constants.BUFF_SIZE_HI) {
				buffSize = Constants.BUFF_SIZE_HI;
			} else {
				buffSize = (int) approxDataItemSize;
			}
		} else {
			if(sizeMin == sizeMax) {
				LOG.debug(Markers.MSG, "Fixed data item size: {}", SizeUtil.formatSize(sizeMin));
				buffSize = sizeMin < Constants.BUFF_SIZE_HI ? (int) sizeMin : Constants.BUFF_SIZE_HI;
			} else {
				final long t = (sizeMin + sizeMax) / 2;
				buffSize = t < Constants.BUFF_SIZE_HI ? (int) t : Constants.BUFF_SIZE_HI;
				LOG.debug(
					Markers.MSG, "Average data item size: {}",
					SizeUtil.formatSize(buffSize)
				);
			}
			if(buffSize < Constants.BUFF_SIZE_LO) {
				LOG.debug(
					Markers.MSG, "Buffer size {} is less than lower bound {}",
					SizeUtil.formatSize(buffSize), SizeUtil.formatSize(Constants.BUFF_SIZE_LO)
				);
				buffSize = Constants.BUFF_SIZE_LO;
			}
		}
		LOG.debug(
			Markers.MSG, "Determined buffer size of {} for \"{}\"",
			SizeUtil.formatSize(buffSize), getName()
		);
		this.reqConfigCopy.setBuffSize(buffSize);
		//
		switch(loadType) {
			case APPEND:
			case CREATE:
				if(sizeMin < 0) {
					throw new IllegalArgumentException(
						"Min data item size is less than zero: " + SizeUtil.formatSize(sizeMin)
					);
				}
				if(sizeMin > sizeMax) {
					throw new IllegalArgumentException(
						"Min object size shouldn't be more than max: " +
						SizeUtil.formatSize(sizeMin) + ", " + SizeUtil.formatSize(sizeMax)
					);
				}
				if(sizeBias < 0) {
					throw new IllegalArgumentException(
						"Object size bias should not be less than 0: " + sizeBias
					);
				}
				break;
			case UPDATE:
				if(countUpdPerReq < 0) {
					throw new IllegalArgumentException(
						"Invalid updates per request count: " + countUpdPerReq
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
	//
	private void scheduleAppend(final T dataItem) {
		final long nextSize = sizeMin +
			(long) (Math.pow(ThreadLocalRandom.current().nextDouble(), sizeBias) * sizeRange);
		try {
			dataItem.scheduleAppend(nextSize);
		} catch(final IllegalArgumentException e) {
			LogUtil.exception(
				LOG, Level.WARN, e,
				"Failed to schedule the append operation for the data item"
			);
		}
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, "Append the object \"{}\": +{}",
				dataItem, SizeUtil.formatSize(nextSize)
			);
		}
	}
	//
	private void scheduleUpdate(final T dataItem) {
		if(dataItem.getSize() > 0) {
			dataItem.scheduleRandomUpdates(countUpdPerReq);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Modified {} ranges for object \"{}\"", countUpdPerReq, dataItem
				);
			}
		} else {
			throw new RejectedExecutionException("It's impossible to update empty data item");
		}
	}
	/** intercepts the data items which should be scheduled for update or append */
	@Override
	public final void put(final T dataItem)
	throws IOException {
		try {
			switch(loadType) {
				case APPEND:
					scheduleAppend(dataItem);
					break;
				case UPDATE:
					scheduleUpdate(dataItem);
					break;
			}
		} catch(final IllegalArgumentException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to schedule {} for the data item",
				loadType.name().toLowerCase()
			);
		}
		//
		super.put(dataItem);
	}
	//
	@Override
	public final int put(final List<T> dataItems, final int from, final int to)
	throws IOException {
		try {
			switch(loadType) {
				case APPEND:
					for(int i = from; i < to; i ++) {
						scheduleAppend(dataItems.get(i));
					}
					break;
				case UPDATE:
					for(int i = from; i < to; i ++) {
						scheduleUpdate(dataItems.get(i));
					}
					break;
			}
		} catch(final IllegalArgumentException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to schedule {} for the data items",
				loadType.name().toLowerCase()
			);
		}
		//
		return super.put(dataItems, from, to);
	}
}
