package com.emc.mongoose.core.impl.load.generator;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.DataRangesConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.core.api.io.task.IoTask;
import com.emc.mongoose.core.api.item.data.MutableDataItem;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.generator.LoadState;
import com.emc.mongoose.core.api.load.generator.MutableDataLoadGenerator;
import org.apache.commons.lang.NotImplementedException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
/**
 Created by kurila on 12.04.16.
 */
public class BasicMutableDataLoadGenerator<T extends MutableDataItem, A extends IoTask<T>>
extends BasicLoadGenerator<T, A>
implements MutableDataLoadGenerator<T, A> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final SizeInBytes sizeConfig;
	private final DataRangesConfig rangesConfig;
	//
	public BasicMutableDataLoadGenerator(
		final AppConfig appConfig, final String name, final LoadType loadType,
		final LoadExecutor<T, A> executor, final Input<T> itemInput, final long countLimit,
		final int weight, final float rateLimit,
		final boolean isCircular, final boolean shuffleFlag,
		final SizeInBytes sizeConfig, final DataRangesConfig rangesConfig
	) {
		this(
			appConfig, name, loadType, executor, itemInput, countLimit, weight, rateLimit,
			isCircular, shuffleFlag, null, sizeConfig, rangesConfig
		);
	}
	//
	public BasicMutableDataLoadGenerator(
		final AppConfig appConfig, final String name, final LoadType loadType,
		final LoadExecutor<T, A> executor, final Input<T> itemInput, final long countLimit,
		final int weight, final float rateLimit,
		final boolean isCircular, final boolean shuffleFlag, final LoadState<T> loadState,
		final SizeInBytes sizeConfig, final DataRangesConfig rangesConfig
	) {
		super(
			appConfig, name, loadType, executor, itemInput, countLimit, weight, rateLimit,
			isCircular, shuffleFlag, loadState
		);
		this.sizeConfig = sizeConfig;
		this.rangesConfig = rangesConfig;
	}
	//
	protected int produceLoadFor(final List<T> items, final int from, final int to)
	throws IOException {
		try {
			final int rndRangesToUpdateCount = rangesConfig.getRandomCount();
			final List<DataRangesConfig.ByteRange> ranges = rangesConfig.getFixedByteRanges();
			if(rndRangesToUpdateCount > 0) {
				for(int i = from; i < to; i ++) {
					items.get(i).scheduleRandomUpdates(rndRangesToUpdateCount);
				}
			} else if(ranges != null) {
				if(ranges.size() == 1) {
					final DataRangesConfig.ByteRange range = ranges.get(0);
					T dataItem;
					for(int i = from; i < to; i ++) {
						dataItem = items.get(i);
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
		return super.produceLoadFor(items, from, to);
	}
}
