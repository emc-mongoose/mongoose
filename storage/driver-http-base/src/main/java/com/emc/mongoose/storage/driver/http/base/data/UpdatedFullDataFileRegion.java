package com.emc.mongoose.storage.driver.http.base.data;

import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.model.impl.item.BasicDataItem;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

import static com.emc.mongoose.model.impl.item.BasicMutableDataItem.getRangeOffset;

public class UpdatedFullDataFileRegion<T extends MutableDataItem>
extends DataItemFileRegion<T> {

	private BasicDataItem currRange;
	private long currRangeSize, nextRangeOffset;
	private int currRangeIdx, currDataLayerIdx;
	private ContentSource contentSource;
	
	public UpdatedFullDataFileRegion(final T dataItem, final ContentSource contentSource)
	throws IOException {
		super(dataItem);
		this.contentSource = contentSource;
		currDataLayerIdx = dataObject.getCurrLayerIndex();
	}

	@Override
	public long transferTo(final WritableByteChannel target, final long position)
	throws IOException {
		dataObject.position(position);
		if(doneByteCount == nextRangeOffset) {
			currRangeSize = dataObject.getRangeSize(currRangeIdx);
			if(dataObject.isCurrLayerRangeUpdated(currRangeIdx)) {
				currRange = new BasicDataItem(
					dataObject.getOffset() + nextRangeOffset, currRangeSize, currDataLayerIdx + 1,
					contentSource
				);
			} else {
				currRange = new BasicDataItem(
					dataObject.getOffset() + nextRangeOffset, currRangeSize, currDataLayerIdx,
					contentSource
				);
			}
			currRangeIdx ++;
			nextRangeOffset = getRangeOffset(currRangeIdx);
		}
		if(currRangeSize > 0) {
			doneByteCount += currRange.write(
				target, Math.min(nextRangeOffset - position, baseItemSize - doneByteCount)
			);
		}
		return doneByteCount;
	}
}
