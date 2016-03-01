package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.Constants;
// mongoose-common.jar
import com.emc.mongoose.common.conf.DataRangesConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.MutableDataItem;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.item.data.DataItemFileSrc;
import com.emc.mongoose.core.api.io.conf.IOConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
//
import com.emc.mongoose.core.impl.item.data.NewDataItemSrc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
/**
 Created by kurila on 15.12.14.
 */
public abstract class MutableDataLoadExecutorBase<T extends MutableDataItem>
extends LimitedRateLoadExecutorBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final AppConfig.LoadType loadType;
	protected final DataRangesConfig rangesConfig;
	//
	protected MutableDataLoadExecutorBase(
		final AppConfig appConfig,
		final IOConfig<? extends DataItem, ? extends Container<? extends DataItem>> ioConfig,
		final String[] addrs, final int threadCount, final ItemSrc<T> itemSrc, final long maxCount,
		final float rateLimit, final DataRangesConfig rangesConfig
	) throws ClassCastException {
		super(appConfig, ioConfig, addrs, threadCount, itemSrc, maxCount, rateLimit);
		//
		this.loadType = ioConfig.getLoadType();
		this.rangesConfig = rangesConfig;
		//
		int buffSize;
		if(itemSrc instanceof DataItemFileSrc) {
			final long avgDataSize = ((DataItemFileSrc) itemSrc).getAvgDataSize(
				appConfig.getItemSrcBatchSize()
			);
			if(avgDataSize < Constants.BUFF_SIZE_LO) {
				buffSize = Constants.BUFF_SIZE_LO;
			} else if(avgDataSize > Constants.BUFF_SIZE_HI) {
				buffSize = Constants.BUFF_SIZE_HI;
			} else {
				buffSize = (int) avgDataSize;
			}
		} else if(itemSrc instanceof NewDataItemSrc) {
			final long avgDataSize = ((NewDataItemSrc) itemSrc).getDataSizeInfo().getAvgDataSize();
			if(avgDataSize < Constants.BUFF_SIZE_LO) {
				buffSize = Constants.BUFF_SIZE_LO;
			} else if(avgDataSize > Constants.BUFF_SIZE_HI) {
				buffSize = Constants.BUFF_SIZE_HI;
			} else {
				buffSize = (int) avgDataSize;
			}
		} else {
			buffSize = Constants.BUFF_SIZE_LO;
		}
		LOG.debug(
			Markers.MSG, "Determined buffer size of {} for \"{}\"",
			SizeInBytes.formatFixedSize(buffSize), getName()
		);
		this.ioConfigCopy.setBuffSize(buffSize);
		/*
		switch(loadType) {
			case WRITE:
				// TODO partial content support
				if(sizeMin < 0) {
					throw new IllegalArgumentException(
						"Min data item size is less than zero: " + SizeInBytes.formatFixedSize(sizeMin)
					);
				}
				if(sizeMin > sizeMax) {
					throw new IllegalArgumentException(
						"Min object size shouldn't be more than max: " +
						SizeInBytes.formatFixedSize(sizeMin) + ", " + SizeInBytes.formatFixedSize(sizeMax)
					);
				}
				if(sizeBias < 0) {
					throw new IllegalArgumentException(
						"Object size bias should not be less than 0: " + sizeBias
					);
				}
				break;
		}
		//
		this.sizeMin = sizeMin;
		sizeRange = sizeMax - sizeMin;
		this.sizeBias = sizeBias;
		//
		this.randomRangeCount = randomRangeCount;*/
	}
	/*
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
				dataItem, SizeInBytes.formatFixedSize(nextSize)
			);
		}
	}
	//
	private void scheduleUpdate(final T dataItem) {
		if(dataItem.getSize() > 0) {
			dataItem.scheduleRandomUpdates(randomRangeCount);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Modified {} ranges for object \"{}\"", randomRangeCount, dataItem
				);
			}
		} else {
			throw new RejectedExecutionException("It's impossible to update empty data item");
		}
	}*/
	/** intercepts the data items which should be scheduled for update or append */
	/*@Override
	public final void put(final T dataItem)
	throws IOException {
		try {
			// TODO schedule partial write
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
			// TODO schedule partial write
		} catch(final IllegalArgumentException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to schedule {} for the data items",
				loadType.name().toLowerCase()
			);
		}
		//
		return super.put(dataItems, from, to);
	}*/
}
