package com.emc.mongoose.object.data.impl;
//
import com.emc.mongoose.base.data.impl.UniformDataSource;
//
import com.emc.mongoose.object.data.WSDataObject;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
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
public final class WSDataObjectBase
extends DataObjectBase
implements WSDataObject {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	public WSDataObjectBase() {
		super();
	}
	//
	public WSDataObjectBase(final String metaInfo) {
		super();
		fromString(metaInfo);
	}
	//
	public WSDataObjectBase(final long size) {
		super(size);
	}
	//
	public WSDataObjectBase(final long size, final UniformDataSource dataSrc) {
		super(size, dataSrc);
	}
	//
	public WSDataObjectBase(final long id, final long size) {
		super(id, size);
	}
	//
	public WSDataObjectBase(final long id, final long size, final UniformDataSource dataSrc) {
		super(id, size, dataSrc);
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
		return new WSUpdateRangesEntity<WSDataObject>(this);
	}
	//
	public final HttpEntity getPendingAugmentContentEntity() {
		return new WSAugmentEntity<WSDataObject>(this);
	}
}
//
