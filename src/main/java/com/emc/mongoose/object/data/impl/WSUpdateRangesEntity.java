package com.emc.mongoose.object.data.impl;
//
//
import com.emc.mongoose.object.data.WSDataObject;
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
final class WSUpdateRangesEntity<T extends WSDataObject>
implements HttpEntity {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	private final T baseItem;
	//
	protected WSUpdateRangesEntity(final T baseItem) {
		this.baseItem = baseItem;
	}
	//
	@Override
	public final boolean isRepeatable() {
		return WSDataObject.IS_CONTENT_REPEATABLE;
	}
	//
	@Override
	public final boolean isChunked() {
		return WSDataObject.IS_CONTENT_CHUNKED;
	}
	//
	@Override
	public final long getContentLength() {
		return baseItem.getPendingUpdatesCount() * baseItem.getRangeSize();
	}
	//
	@Override
	public final Header getContentType() {
		return WSDataObject.HEADER_CONTENT_TYPE;
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
		baseItem.writePendingUpdatesTo(out);
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
