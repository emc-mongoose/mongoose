package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.Constants;
// mongoose-common.jar
import com.emc.mongoose.common.conf.DataRangesConfig;
import com.emc.mongoose.common.conf.DataRangesConfig.ByteRange;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.MutableDataItem;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.item.data.DataItemFileSrc;
import com.emc.mongoose.core.api.io.conf.IOConfig;
//
import com.emc.mongoose.core.impl.item.data.NewDataItemSrc;
import org.apache.commons.lang.NotImplementedException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
//
/**
 Created by kurila on 15.12.14.
 */
public abstract class MutableDataLoadExecutorBase<T extends MutableDataItem>
extends LoadExecutorBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final LoadType loadType;
	protected final SizeInBytes sizeConfig;
	protected final DataRangesConfig rangesConfig;
	//
	protected MutableDataLoadExecutorBase(
		final AppConfig appConfig,
		final IOConfig<? extends DataItem, ? extends Container<? extends DataItem>> ioConfig,
		final String[] addrs, final int threadCount, final ItemSrc<T> itemSrc, final long maxCount,
		final float rateLimit, final SizeInBytes sizeConfig, final DataRangesConfig rangesConfig
	) throws ClassCastException {
		super(appConfig, ioConfig, addrs, threadCount, itemSrc, maxCount, rateLimit);
		//
		this.loadType = ioConfig.getLoadType();
		this.sizeConfig = sizeConfig;
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
	@Override
	public void put(final T item)
	throws IOException {
		try {
			final int rndRangesToUpdateCount = rangesConfig.getRandomCount();
			final List<ByteRange> ranges = rangesConfig.getFixedByteRanges();
			if(rndRangesToUpdateCount > 0) {
				item.scheduleRandomUpdates(rndRangesToUpdateCount);
			} else if(ranges != null) {
				if(ranges.size() == 1) {
					final ByteRange range = ranges.get(0);
					if(range.getBeg() == item.getSize()) {
						item.scheduleAppend(range.getEnd());
					} else {
						// TODO
						throw new NotImplementedException();
					}
				} else {
					// TODO
					throw new NotImplementedException();
				}
			}
		} catch(final IllegalArgumentException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to schedule {} for the data item",
				loadType.name().toLowerCase()
			);
		}
		//
		super.put(item);
	}
	//
	@Override
	public int put(final List<T> dataItems, final int from, final int to)
	throws IOException {
		try {
			final int rndRangesToUpdateCount = rangesConfig.getRandomCount();
			final List<ByteRange> ranges = rangesConfig.getFixedByteRanges();
			if(rndRangesToUpdateCount > 0) {
				for(int i = from; i < to; i ++) {
					dataItems.get(i).scheduleRandomUpdates(rndRangesToUpdateCount);
				}
			} else if(ranges != null) {
				if(ranges.size() == 1) {
					final ByteRange range = ranges.get(0);
					T dataItem;
					for(int i = from; i < to; i ++) {
						dataItem = dataItems.get(i);
						if(range.getBeg() == dataItem.getSize()) {
							dataItem.scheduleAppend(range.getEnd());
						} else {
							// TODO
							throw new NotImplementedException();
						}
					}
				} else {
					// TODO
					throw new NotImplementedException();
				}
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
