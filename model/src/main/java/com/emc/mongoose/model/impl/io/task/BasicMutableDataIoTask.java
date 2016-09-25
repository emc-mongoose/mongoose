package com.emc.mongoose.model.impl.io.task;

import com.emc.mongoose.model.api.io.task.MutableDataIoTask;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.model.api.item.UpdatableDataItem;
import com.emc.mongoose.model.impl.item.BasicDataItem;
import com.emc.mongoose.model.util.LoadType;

import java.io.IOException;

/**
 Created by andrey on 25.09.16.
 */
public final class BasicMutableDataIoTask<I extends MutableDataItem>
extends BasicDataIoTask<I>
implements MutableDataIoTask<I> {

	private volatile BasicDataItem currRange = null;
	private volatile long nextRangeOffset;
	private volatile int currRangeIdx;
	private volatile int currDataLayerIdx;

	public BasicMutableDataIoTask(final LoadType ioType, final I item, final String dstPath)
	throws IOException {
		super(ioType, item, dstPath);
		if(LoadType.UPDATE.equals(ioType)) {
			if(item.hasScheduledUpdates()) {
				contentSize = item.getUpdatingRangesSize();
			} else if(item.isAppending()) {
				contentSize = item.getAppendSize();
			} else {
				contentSize = item.size();
			}
		}
	}

	@Override
	public final void reset() {
		super.reset();
		currRange = null;
		nextRangeOffset = 0;
		currRangeIdx = 0;
		currDataLayerIdx = 0;
	}

	@Override
	public final BasicDataItem getCurrRange() {
		if(currRange == null && currRangeIdx < item.getCountRangesTotal()) {
			final long currRangeSize = item.getRangeSize(currRangeIdx);
			nextRangeOffset = UpdatableDataItem.getRangeOffset(currRangeIdx + 1);
			if(item.isCurrLayerRangeUpdated(currRangeIdx)) {
				currRange = new BasicDataItem(
					item.getOffset() + nextRangeOffset, currRangeSize,
					currDataLayerIdx + 1, item.getContentSrc()
				);
			} else {
				currRange = new BasicDataItem(
					item.getOffset() + nextRangeOffset, currRangeSize,
					currDataLayerIdx, item.getContentSrc()
				);
			}
		}
		return currRange;
	}

	@Override
	public BasicDataItem getUpdatingRange() {
		if(currRange == null) {
			final long currRangeSize = item.getRangeSize(currRangeIdx);
			nextRangeOffset = UpdatableDataItem.getRangeOffset(currRangeIdx + 1);
			if(item.isCurrLayerRangeUpdating(currRangeIdx)) {
				currRange = new BasicDataItem(
					item.getOffset() + nextRangeOffset, currRangeSize,
					currDataLayerIdx + 1, item.getContentSrc()
				);
			} else if(item.isNextLayerRangeUpdating(currRangeIdx)) {
				currRange = new BasicDataItem(
					item.getOffset() + nextRangeOffset, currRangeSize,
					currDataLayerIdx + 2, item.getContentSrc()
				);
			}
		}
		return currRange;
	}

	@Override
	public final long getNextRangeOffset() {
		return nextRangeOffset;
	}

	@Override
	public final int getCurrRangeIdx() {
		return currRangeIdx;
	}

	@Override
	public final void incrementRangeIdx() {
		currDataLayerIdx ++;
	}
}
