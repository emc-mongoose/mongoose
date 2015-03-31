package com.emc.mongoose.storage.mock.impl.cinderella;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.io.HTTPInputStream;
import com.emc.mongoose.common.io.StreamUtils;
import com.emc.mongoose.common.logging.TraceLogger;
//
import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ContentBufferEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.nio.util.SimpleInputBuffer;
import org.apache.http.protocol.HttpContext;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.InputStream;
//
/**
 * Created by olga on 04.02.15.
 */
public final class BasicRequestConsumer
extends BasicAsyncRequestConsumer {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private volatile HttpRequest request;
	private volatile SimpleInputBuffer buf;
	//
	private final int buffSize;
	//
	public BasicRequestConsumer() {
		super();
		buffSize = (int) RunTimeConfig.getContext().getDataBufferSize();
	}
	//
	@Override
	protected final void onRequestReceived(final HttpRequest request) throws IOException {
		this.request = request;
	}
	//
	@Override
	protected final void onEntityEnclosed(
		final HttpEntity entity, final ContentType contentType)
	{
		long len = entity.getContentLength();
		//
		if (len < 0 || len > buffSize) {
			len = buffSize;
		}
		this.buf = new SimpleInputBuffer(
			len > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) len, new HeapByteBufferAllocator()
		);
		((HttpEntityEnclosingRequest) this.request).setEntity(
			new ContentBufferEntity(entity, this.buf));
	}
	//
	@Override
	protected final void onContentReceived(final ContentDecoder decoder, final IOControl ioctrl) {
		HTTPInputStream.consumeQuietly(decoder, ioctrl, buffSize);
	}
	//
	@Override
	protected final void releaseResources() {
		this.request = null;
		this.buf = null;
	}
	//
	@Override
	protected final  HttpRequest buildResult(final HttpContext context) {
		return this.request;
	}
}
