package com.emc.mongoose.object.http.data;
//
import com.emc.mongoose.conf.RunTimeConfig;
import com.emc.mongoose.data.UniformDataSource;
import com.emc.mongoose.object.DataObject;
//
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
/**
 Created by kurila on 29.04.14.
 */
public final class WSObject
extends DataObject
implements HttpEntity {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	public WSObject() {
		super();
	}
	//
	public WSObject(final String metaInfo) {
		super();
		fromString(metaInfo);
	}
	//
	public WSObject(final long size) {
		super(size);
	}
	//
	public WSObject(final long size, final UniformDataSource dataSrc) {
		super(size, dataSrc);
	}
	//
	public WSObject(final long id, final long size) {
		super(id, size);
	}
	//
	public WSObject(final long id, final long size, final UniformDataSource dataSrc) {
		super(id, size, dataSrc);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// HttpEntity interface implementation
	////////////////////////////////////////////////////////////////////////////////////////////////
	public final static Header
		HEADER_CONTENT_TYPE = new BasicHeader(
			HttpHeaders.CONTENT_TYPE, RunTimeConfig.getString("http.content.type")
		);
	public final static boolean
		IS_CONTENT_REPEATABLE = RunTimeConfig.getBoolean("http.content.repeatable"),
		IS_CONTENT_CHUNKED = RunTimeConfig.getBoolean("http.content.chunked");
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
	public final long getContentLength() {
		return size;
	}
	//
	@Override
	public final Header getContentType() {
		return HEADER_CONTENT_TYPE;
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
		return this;
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
	//
	public final HttpEntity getPendingUpdatesContentEntity() {
		return new WSUpdateRangesEntity(this);
	}
	//
	public final HttpEntity getPendingAugmentContentEntity() {
		return new WSAugmentEntity(this);
	}
}
//
