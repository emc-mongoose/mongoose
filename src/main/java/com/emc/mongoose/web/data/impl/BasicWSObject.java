package com.emc.mongoose.web.data.impl;
//
import com.emc.mongoose.base.data.impl.UniformDataSource;
import com.emc.mongoose.object.data.impl.BasicObject;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.web.data.WSObject;
//
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.util.EntityUtils;
//
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
//
//import org.apache.logging.log4j.Level;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
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
	public BasicWSObject(final Long size, final UniformDataSource dataSrc) {
		super(size, dataSrc);
	}
	//
	public BasicWSObject(final String id, final Long size) {
		super(id, size);
	}
	//
	public BasicWSObject(final String id, final Long offset, final Long size) {
		super(id, offset, size);
	}
	//
	public BasicWSObject(final String id, final Long size, final UniformDataSource dataSrc) {
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
		return new UpdateRangesEntity<WSObject>(this);
	}
	//
	public final HttpEntity getPendingAugmentContentEntity() {
		return new AugmentEntity<WSObject>(this);
	}
	//
	@Override
	public final boolean compareWith(final ContentDecoder in, final IOControl ioCtl) {
		return true;
	}
	//
	@Override
	public void updateRanges(List<Long> ranges) {
	}
}
//
