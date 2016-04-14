package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.core.api.item.data.ContentSource;
import com.emc.mongoose.core.api.item.data.MutableDataItem;
import com.emc.mongoose.core.impl.item.data.BasicDataItem;
import com.emc.mongoose.core.impl.item.data.BasicMutableDataItem;
import com.emc.mongoose.core.impl.item.data.ContentSourceBase;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public final class ZeroCopyUpdatedDataItemWrapper<T extends MutableDataItem>
extends ZeroCopyDataItemWrapper<T> {

	private BasicDataItem currRange;
	private long currRangeSize, nextRangeOffset;
	private int currRangeIdx, currDataLayerIdx;
	private final ContentSource contentSource;

	public ZeroCopyUpdatedDataItemWrapper(final T object) {
		super(object);
		currDataLayerIdx = this.object.getCurrLayerIndex();
		contentSource = ContentSourceBase.getDefaultInstance();
	}

	@Override
	public final long transferTo(final WritableByteChannel tgtChan, final long position)
	throws IOException {
		object.setRelativeOffset(position);
		if (doneByteCount == nextRangeOffset) {
			currRangeSize = object.getRangeSize(currRangeIdx);
			if (object.isCurrLayerRangeUpdated(currRangeIdx)) {
				currRange = new BasicDataItem(
					object.getOffset() + nextRangeOffset, currRangeSize, currDataLayerIdx + 1,
					contentSource
				);
			} else {
				currRange = new BasicDataItem(
					object.getOffset() + nextRangeOffset, currRangeSize, currDataLayerIdx,
					contentSource
				);
			}
			currRangeIdx++;
			nextRangeOffset = BasicMutableDataItem.getRangeOffset(currRangeIdx);
		}
		if (currRangeSize > 0) {
			doneByteCount += currRange.write(
				tgtChan, Math.min(nextRangeOffset - position, size - doneByteCount)
			);
		}
		return doneByteCount;
	}

}
