package com.emc.mongoose.storage.mock.impl.http.request;

import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.impl.data.ContentSourceUtil;
import com.emc.mongoose.model.impl.item.BasicDataItem;
import com.emc.mongoose.model.impl.item.BasicMutableDataItem;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.ui.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class UpdatedDataItemFileRegion<T extends MutableDataItemMock> extends DataItemFileRegion<T> {

    private final static Logger LOG = LogManager.getLogger();


    private BasicDataItem currRange;
    private long currRangeSize, nextRangeOffset;
    private int currRangeIdx, currDataLayerIdx;
    private ContentSource contentSource;


    public UpdatedDataItemFileRegion(
            final T dataItem, final ContentSource contentSource) {
        super(dataItem);
        this.contentSource = contentSource;
        currDataLayerIdx = dataObject.getCurrLayerIndex();
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
