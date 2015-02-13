package com.emc.mongoose.web.mock;
//
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.io.HTTPContentInputStream;
import com.emc.mongoose.util.logging.TraceLogger;
import com.emc.mongoose.web.api.impl.WSRequestConfigBase;
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
public final class CinderellaBasicAsyncRequestConsumer
extends BasicAsyncRequestConsumer {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private volatile HttpRequest request;
	private volatile SimpleInputBuffer buf;
	//
	private final int maxPageSize;
	//
	public CinderellaBasicAsyncRequestConsumer(final RunTimeConfig runTimeConfig) {
		super();
		maxPageSize = (int) runTimeConfig.getDataPageSize();
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
		if (len < 0 || len > maxPageSize) {
			len = maxPageSize;
		}
		this.buf = new SimpleInputBuffer((int) len, new HeapByteBufferAllocator());
		((HttpEntityEnclosingRequest) this.request).setEntity(
			new ContentBufferEntity(entity, this.buf));
	}
	//
	@Override
	protected final void onContentReceived(
		final ContentDecoder decoder, final IOControl ioctrl)
	{
		//this.buf.consumeContent(decoder);
		try (final InputStream contentStream = HTTPContentInputStream.getInstance(decoder, ioctrl)) {
			WSRequestConfigBase.playStreamQuietly(contentStream);
			this.buf.shutdown();
		} catch (final InterruptedException e) {
			TraceLogger.failure(LOG, Level.ERROR, e, "Buffer interrupted fault");
		} catch (final IOException e){
			TraceLogger.failure(LOG, Level.ERROR, e, "Content input stream fault");
		}
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
