package com.emc.mongoose.object.data.impl;
//
import com.emc.mongoose.object.data.WSObject;
//
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
//
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
/**
 Created by kurila on 23.09.14.
 A web storage object append HTTP entity.
 */
final class AugmentEntity<T extends WSObject>
implements HttpEntity {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	private final T baseItem;
	//
	protected AugmentEntity(final T baseItem) {
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
		return baseItem.getPendingAugmentSize();
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
	throws IOException {
		return baseItem.getAugmentContent();
	}
	//
	@Override
	public final void writeTo(final OutputStream out)
		throws IOException {
		baseItem.writeAugmentTo(out);
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
