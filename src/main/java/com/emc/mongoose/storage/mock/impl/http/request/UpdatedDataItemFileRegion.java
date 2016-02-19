package com.emc.mongoose.storage.mock.impl.http.request;

import com.emc.mongoose.core.api.item.data.ContentSource;
import com.emc.mongoose.core.impl.item.data.BasicDataItem;
import com.emc.mongoose.core.impl.item.data.BasicMutableDataItem;
import com.emc.mongoose.core.impl.item.data.ContentSourceBase;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class UpdatedDataItemFileRegion<T extends MutableDataItemMock> extends DataItemFileRegion<T> {

	private BasicDataItem currRange;
	private long currRangeSize, nextRangeOffset;
	private int currRangeIdx, currDataLayerIdx;
	private final ContentSource contentSource;


	public UpdatedDataItemFileRegion(T dataItem) {
		super(dataItem);
		currDataLayerIdx = dataObject.getCurrLayerIndex();
		contentSource = ContentSourceBase.getDefault();
	}

	@Override
	public long transferTo(WritableByteChannel target, long position)
			throws IOException {
		dataObject.setRelativeOffset(position);
		if (doneByteCount == nextRangeOffset) {
			currRangeSize = dataObject.getRangeSize(currRangeIdx);
			if (dataObject.isCurrLayerRangeUpdated(currRangeIdx)) {
				currRange = new BasicDataItem(
						dataObject.getOffset() + nextRangeOffset, currRangeSize, currDataLayerIdx + 1, contentSource
				);
			} else {
				currRange = new BasicDataItem(
						dataObject.getOffset() + nextRangeOffset, currRangeSize, currDataLayerIdx, contentSource
				);
			}
			currRangeIdx++;
			nextRangeOffset = BasicMutableDataItem.getRangeOffset(currRangeIdx);
		}
		if (currRangeSize > 0) {
			doneByteCount += currRange.write(target, Math.min(nextRangeOffset - position, dataSize - doneByteCount));
		}
		return doneByteCount;
	}

}
