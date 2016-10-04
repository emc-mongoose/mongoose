package com.emc.mongoose.storage.driver.net.base.data;

import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.model.impl.item.BasicDataItem;
import com.emc.mongoose.storage.driver.net.base.data.DataItemFileRegion;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

import static com.emc.mongoose.model.api.item.MutableDataItem.getRangeOffset;
import static java.lang.Math.min;

public class UpdatedFullDataFileRegion<T extends MutableDataItem>
extends DataItemFileRegion<T> {

	private DataItem currRange;
	private long currRangeSize, nextRangeOffset, dataItemOffset;
	private final int layerNum;
	private final ContentSource contentSrc;
	
	private int currRangeIdx = 0;
	
	public UpdatedFullDataFileRegion(final T dataItem)
	throws IOException {
		super(dataItem);
		dataItemOffset = dataItem.offset();
		layerNum = dataItem.layer();
		contentSrc = dataItem.getContentSrc();
	}

	@Override
	public long transferTo(final WritableByteChannel target, final long position)
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
