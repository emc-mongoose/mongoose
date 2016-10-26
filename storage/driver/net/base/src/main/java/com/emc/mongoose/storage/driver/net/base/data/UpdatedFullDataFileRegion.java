package com.emc.mongoose.storage.driver.net.base.data;

import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.item.MutableDataItem;
import com.emc.mongoose.model.item.BasicDataItem;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

import static com.emc.mongoose.model.item.MutableDataItem.getRangeOffset;
import static java.lang.Math.min;

public final class UpdatedFullDataFileRegion<I extends MutableDataItem>
extends DataItemFileRegion<I> {

	private DataItem currRange;
	private long currRangeSize, nextRangeOffset, dataItemOffset;
	private final int layerNum;
	private final ContentSource contentSrc;
	
	private int currRangeIdx = 0;
	
	public UpdatedFullDataFileRegion(final I dataItem)
	throws IOException {
		super(dataItem);
		dataItemOffset = dataItem.offset();
		layerNum = dataItem.layer();
		contentSrc = dataItem.getContentSrc();
	}

	@Override
	public final long transferTo(final WritableByteChannel target, final long position)
	throws IOException {
		dataItem.position(position);
		if(doneByteCount == nextRangeOffset) {
			currRangeSize = dataItem.getRangeSize(currRangeIdx);
			if(dataItem.isRangeUpdated(currRangeIdx)) {
				currRange = new BasicDataItem(
					dataItemOffset + nextRangeOffset, currRangeSize, layerNum + 1, contentSrc
				);
			} else {
				currRange = new BasicDataItem(
					dataItemOffset + nextRangeOffset, currRangeSize, layerNum, contentSrc
				);
			}
			currRangeIdx ++;
			nextRangeOffset = getRangeOffset(currRangeIdx);
		}
		if(currRangeSize > 0) {
			doneByteCount += currRange.write(
				target, min(nextRangeOffset - position, baseItemSize - doneByteCount)
			);
		}
		return doneByteCount;
	}
}
