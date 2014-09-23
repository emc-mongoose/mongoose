package com.emc.mongoose.object.http.data;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
/**
 Created by kurila on 23.09.14.
 */
public class WSAppendEntity
implements HttpEntity {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	private final WSObject baseItem;
	//
	protected WSAppendEntity(final WSObject baseItem) {
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
		baseItem.writeAppendTo(out);
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
