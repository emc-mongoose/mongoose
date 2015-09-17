package com.emc.mongoose.core.impl.data;
// mongoose-core-api
import com.emc.mongoose.core.api.data.WSObject;
//
import org.apache.http.Header;
import org.apache.http.util.EntityUtils;
//
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
//
//import org.apache.log.log4j.Level;
//import org.apache.log.log4j.LogManager;
//import org.apache.log.log4j.Logger;
//
/**
 Created by kurila on 29.04.14.
 Basic web storage data object implementation.
 */
public class BasicWSObject
extends BasicObject
implements WSObject {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	public BasicWSObject() {
		super();
	}
	//
	public BasicWSObject(final String metaInfo) {
		super();
		fromString(metaInfo);
	}
	//
	public BasicWSObject(final Long size) {
		super(size);
	}
	//
	public BasicWSObject(final String id, final Long offset, final Long size) {
		super(id, offset, size);
	}
	//
	@Override
	public final boolean isRepeatable() {
		return IS_CONTENT_REPEATABLE;
	}
	//
	@Override
	public final boolean isChunked() {
		return IS_CONTENT_CHUNKED;
	}
	//
	@Override
	public long getContentLength() {
		if(hasScheduledUpdates()) {
			return getUpdatingRangesSize();
		} else if(isAppending()) {
			return getAppendSize();
		} else {
			return size;
		}
	}
	//
	@Override
	public final Header getContentType() {
		return HEADER_CONTENT_TYPE;
	}
	//
	@Override
	public final Header getContentEncoding() {
		return null; // null is ok here
	}
	//
	@Override
	public final InputStream getContent() {
		throw new UnsupportedOperationException("Shouldn't be invoked");
	}
	//
	@Override
	public void writeTo(final OutputStream outstream) {
		throw new UnsupportedOperationException("Shouldn't be invoked");
	}
	//
	@Override
	public boolean isStreaming() {
		return true;
	}
	//
	@Override @Deprecated
	public final void consumeContent()
	throws IOException {
		EntityUtils.consume(this);
	}
}
//
