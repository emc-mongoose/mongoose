package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.common.conf.AppConfig;
// mongoose-common.jar
import com.emc.mongoose.common.conf.DataRangesConfig;
import com.emc.mongoose.common.conf.DataRangesConfig.ByteRange;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.MutableDataItem;
import com.emc.mongoose.core.api.item.data.FileDataItemInput;
import com.emc.mongoose.core.api.io.conf.IoConfig;
//
import com.emc.mongoose.core.api.load.executor.DataLoadExecutor;
import com.emc.mongoose.core.impl.item.data.NewDataItemInput;
import org.apache.commons.lang.NotImplementedException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_HI;
import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_LO;
/**
 Created by kurila on 15.12.14.
 */
public abstract class DataLoadExecutorBase<T extends MutableDataItem>
extends LoadExecutorBase<T>
implements DataLoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final LoadType loadType;
	protected final SizeInBytes sizeConfig;
	protected final DataRangesConfig rangesConfig;
	//
	private final AtomicLong submSize = new AtomicLong(0);
	//
	protected DataLoadExecutorBase(
		final AppConfig appConfig,
		final IoConfig<? extends DataItem, ? extends Container<? extends DataItem>> ioConfig,
		final String[] addrs, final int threadCount, final Input<T> itemInput,
		final long countLimit, final long sizeLimit, final float rateLimit,
		final SizeInBytes sizeConfig, final DataRangesConfig rangesConfig
	) throws ClassCastException {
		super(appConfig, ioConfig, addrs, threadCount, itemInput, countLimit, sizeLimit, rateLimit);
		//
		this.loadType = ioConfig.getLoadType();
		this.sizeConfig = sizeConfig;
		this.rangesConfig = rangesConfig;
		//
		if(itemInput instanceof FileDataItemInput) {
			final int buffSize;
			final long avgDataSize = ((FileDataItemInput) itemInput).getAvgDataSize(
				appConfig.getItemSrcBatchSize()
			);
			if(avgDataSize < BUFF_SIZE_LO) {
				buffSize = BUFF_SIZE_LO;
			} else if(avgDataSize > BUFF_SIZE_HI) {
				buffSize = BUFF_SIZE_HI;
			} else {
				buffSize = (int) avgDataSize;
			}
			LOG.debug(
				Markers.MSG, "Determined buffer size of {} for \"{}\"",
				SizeInBytes.formatFixedSize(buffSize), getName()
			);
			this.ioConfigCopy.setBuffSize(buffSize);
		} else if(itemInput instanceof NewDataItemInput) {
			final int buffSize;
			final long avgDataSize = ((NewDataItemInput)itemInput)
				.getDataSizeInfo().getAvgDataSize();
			if(avgDataSize < BUFF_SIZE_LO) {
				buffSize = BUFF_SIZE_LO;
			} else if(avgDataSize > BUFF_SIZE_HI) {
				buffSize = BUFF_SIZE_HI;
			} else {
				buffSize = (int)avgDataSize;
			}
			LOG.debug(
				Markers.MSG, "Determined buffer size of {} for \"{}\"",
				SizeInBytes.formatFixedSize(buffSize), getName()
			);
			this.ioConfigCopy.setBuffSize(buffSize);
		}
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
	public void put(final T dataItem)
	throws IOException {
		try {
			final int rndRangesToUpdateCount = rangesConfig.getRandomCount();
			final List<ByteRange> ranges = rangesConfig.getFixedByteRanges();
			if(rndRangesToUpdateCount > 0) {
				dataItem.scheduleRandomUpdates(rndRangesToUpdateCount);
				if(sizeLimit < submSize.addAndGet(dataItem.getUpdatingRangesSize())) {
					shutdown();
				}
			} else if(ranges != null) {
				if(ranges.size() == 1) {
					final ByteRange range = ranges.get(0);
					if(range.getBeg() == dataItem.getSize()) {
						final long augmentSize = range.getEnd() - range.getBeg();
						dataItem.scheduleAppend(augmentSize);
						if(sizeLimit < submSize.addAndGet(augmentSize)) {
							shutdown();
						}
					} else {
						// TODO
						throw new NotImplementedException();
					}
				} else {
					// TODO
					throw new NotImplementedException();
				}
			} else {
				if(sizeLimit < submSize.addAndGet(dataItem.getSize())) {
					shutdown();
				}
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
	public int put(final List<T> dataItems, final int from, final int to)
	throws IOException {
		if(sizeLimit < submSize.get()) {
			shutdown();
		}
		T dataItem;
		int toSizeLimit = to;
		try {
			final int rndRangesToUpdateCount = rangesConfig.getRandomCount();
			final List<ByteRange> ranges = rangesConfig.getFixedByteRanges();
			if(rndRangesToUpdateCount > 0) {
				for(int i = from; i < to; i ++) {
					dataItem = dataItems.get(i);
					dataItem.scheduleRandomUpdates(rndRangesToUpdateCount);
					if(sizeLimit < submSize.addAndGet(dataItem.getUpdatingRangesSize())) {
						toSizeLimit = i;
						break;
					}
				}
			} else if(ranges != null) {
				if(ranges.size() == 1) {
					final ByteRange range = ranges.get(0);
					long augmentSize;
					for(int i = from; i < to; i ++) {
						dataItem = dataItems.get(i);
						if(range.getBeg() == dataItem.getSize()) {
							augmentSize = range.getEnd() - range.getBeg();
							dataItem.scheduleAppend(augmentSize);
							if(sizeLimit < submSize.addAndGet(augmentSize)) {
								toSizeLimit = i;
								break;
							}
						} else {
							// TODO
							throw new NotImplementedException();
						}
					}
				} else {
					// TODO
					throw new NotImplementedException();
				}
			} else {
				for(int i = from; i < to; i ++) {
					dataItem = dataItems.get(i);
					if(sizeLimit < submSize.addAndGet(dataItem.getSize())) {
						toSizeLimit = i;
						break;
					}
				}
			}
		} catch(final IllegalArgumentException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to schedule {} for the data items",
				loadType.name().toLowerCase()
			);
		}
		//
		return super.put(dataItems, from, toSizeLimit);
	}
}
