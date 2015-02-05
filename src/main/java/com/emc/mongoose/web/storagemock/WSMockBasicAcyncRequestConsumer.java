package com.emc.mongoose.web.storagemock;

import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.io.HTTPContentInputStream;
import com.emc.mongoose.web.api.impl.WSRequestConfigBase;
import org.apache.http.ContentTooLongException;
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
import org.apache.http.util.Asserts;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by olga on 04.02.15.
 */
public class WSMockBasicAcyncRequestConsumer
extends BasicAsyncRequestConsumer {
	//
	private volatile HttpRequest request;
	private volatile SimpleInputBuffer buf;
	private static int MAX_PAGE_SIZE;
	private final RunTimeConfig runTimeConfig;
	//
	public WSMockBasicAcyncRequestConsumer(final RunTimeConfig runTimeConfig) {
		super();
		MAX_PAGE_SIZE = (int) runTimeConfig.getDataPageSize();
		this.runTimeConfig = runTimeConfig;
	}
	//
	@Override
	protected void onRequestReceived(final HttpRequest request) throws IOException {
		this.request = request;
	}
	//
	@Override
	protected void onEntityEnclosed(
		final HttpEntity entity, final ContentType contentType) throws IOException {
		long len = entity.getContentLength();
		//
		if (len < 0 || len > MAX_PAGE_SIZE) {
			len = 4096;
		}
		this.buf = new SimpleInputBuffer((int) len, new HeapByteBufferAllocator());
		((HttpEntityEnclosingRequest) this.request).setEntity(
			new ContentBufferEntity(entity, this.buf));
	}
	//
	@Override
	protected void onContentReceived(
		final ContentDecoder decoder, final IOControl ioctrl) throws IOException {
		Asserts.notNull(this.buf, "Content buffer");
		//this.buf.consumeContent(decoder);
		try (final InputStream contentStream = HTTPContentInputStream.getInstance(decoder, ioctrl)) {
			WSRequestConfigBase.consumeContentQuetly(contentStream, ioctrl);
			this.buf.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
		//
	@Override
	protected void releaseResources() {
		this.request = null;
		this.buf = null;
	}
	//
	@Override
	protected HttpRequest buildResult(final HttpContext context) {
		return this.request;
	}
}
