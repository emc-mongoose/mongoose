package com.emc.mongoose.object.http.data;
//
import com.emc.mongoose.data.Ranges;
import com.emc.mongoose.data.UniformData;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
/**
 Created by andrey on 25.07.14.
 */
public final class WSRanges
extends Ranges
implements HttpEntity {
	//
	public WSRanges(final long parentSize) {
		super(parentSize);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// HttpEntity interface implementation
	////////////////////////////////////////////////////////////////////////////////////////////////
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
		return getPendingByteCount();
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
		InputStream contentInStream = null;
		//
		UniformData nextRange;
		for(final long nextParentOffset: pendingQueue) {
			nextRange = get(nextParentOffset);
			if(contentInStream==null) { // set head
				contentInStream = nextRange;
			} else { // append
				contentInStream = new SequenceInputStream(contentInStream, nextRange);
			}
		}
		//
		return contentInStream;
	}
	//
	@Override
	public final void writeTo(final OutputStream out)
	throws IOException {
		for(final long nextParentOffset: pendingQueue) {
			get(nextParentOffset).writeTo(out);
		}
		pendingQueue.clear();
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
