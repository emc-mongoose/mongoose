package com.emc.mongoose.object.http.data;
//
import com.emc.mongoose.data.UniformData;
import com.emc.mongoose.data.UniformDataSource;
//
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
//
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
/**
 Created by kurila on 15.09.14.
 */
final class WSUpdateRangesEntity
implements HttpEntity {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	private final WSObject baseItem;
	//
	protected WSUpdateRangesEntity(final WSObject baseItem) {
		this.baseItem = baseItem;
	}
	//
	@Override
	public final boolean isRepeatable() {
		return WSObject.IS_CONTENT_REPEATABLE;
	}
	//
	@Override
	public final boolean isChunked() {
		return WSObject.IS_CONTENT_CHUNKED;
	}
	//
	@Override
	public final long getContentLength() {
		return baseItem.getPendingUpdatesCount() * baseItem.getRangeSize();
	}
	//
	@Override
	public final Header getContentType() {
		return WSObject.HEADER_CONTENT_TYPE;
	}
	//
	@Override
	public final Header getContentEncoding() {
		return null;
	}
	//
	@Override
	public final InputStream getContent()
	throws IOException, IllegalStateException {
		throw new IllegalStateException("Shouldn't be invoked");
		//return baseItem.getPendingUpdatesContent();
	}
	//
	@Override
	public final void writeTo(final OutputStream out)
	throws IOException {
		UniformData nextRangeData;
		final long baseOffset = baseItem.getOffset();
		final int rangeSize = baseItem.getRangeSize();
		long rangeOffset;
		for(int i=0; i<baseItem.getCountRangesTotal(); i++) {
			rangeOffset = i * rangeSize;
			if(baseItem.isRangeUpdatePending(i)) {
				nextRangeData = new UniformData(
					baseOffset + rangeOffset, rangeSize, UniformDataSource.DATA_SRC_UPDATE
				);
				nextRangeData.writeTo(out);
			}
		}
		baseItem.movePendingUpdatesToHistory();
	}
	//
	@Override
	public final boolean isStreaming() {
		return true;
	}
	//
	@Override @Deprecated
	public final void consumeContent()
		throws IOException {
		EntityUtils.consume(this);
	}
	//
}
