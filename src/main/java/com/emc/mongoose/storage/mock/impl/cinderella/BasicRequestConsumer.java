package com.emc.mongoose.storage.mock.impl.cinderella;
//
import com.emc.mongoose.common.collections.InstancePool;
import com.emc.mongoose.common.collections.Reusable;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.io.HTTPInputStream;
//
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.protocol.AbstractAsyncRequestConsumer;
import org.apache.http.protocol.HttpContext;
//
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ContentBufferEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.nio.util.SimpleInputBuffer;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
//
/**
 * Created by olga on 04.02.15.
 */
public final class BasicRequestConsumer
extends AbstractAsyncRequestConsumer<HttpRequest>
implements Reusable<BasicRequestConsumer> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private volatile HttpRequest request = null;
	private volatile SimpleInputBuffer buff = null;
	//
	private int buffSize;
	//
	public BasicRequestConsumer() {
		super();
		buffSize = (int) RunTimeConfig.getContext().getDataBufferSize();
		buff = new SimpleInputBuffer(buffSize, new HeapByteBufferAllocator());
	}
	/*
	public BasicRequestConsumer(final HttpRequest request) {
		this();
		this.request = request;
	}*/
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
		final long contentLen = entity.getContentLength();
		//
		if(contentLen != buffSize) {
			if(contentLen < LoadExecutor.BUFF_SIZE_LO) {
				buffSize = LoadExecutor.BUFF_SIZE_LO;
			} else if(contentLen > LoadExecutor.BUFF_SIZE_HI) {
				buffSize = LoadExecutor.BUFF_SIZE_HI;
			} else {
				buffSize = (int) contentLen;
			}
			buff = new SimpleInputBuffer(buffSize, new HeapByteBufferAllocator());
		}
		HttpEntityEnclosingRequest.class.cast(request).setEntity(
			new ContentBufferEntity(entity, buff)
		);
	}
	//
	@Override
	protected final void onContentReceived(final ContentDecoder decoder, final IOControl ioctrl)
	throws IOException {
		buff.consumeContent(decoder);
		//HTTPInputStream.consumeQuietly(decoder, ioctrl, buffSize);
	}
	//
	@Override
	protected final void releaseResources() {
		this.request = null;
		release();
	}
	//
	@Override
	protected final  HttpRequest buildResult(final HttpContext context) {
		return request;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Reusable implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static InstancePool<BasicRequestConsumer> POOL = new InstancePool<>(
		BasicRequestConsumer.class
	);
	//
	public static BasicRequestConsumer getInstance() {
		return POOL.take();
	}
	//
	@Override
	public final Reusable<BasicRequestConsumer> reuse(final Object... args)
	throws IllegalArgumentException, IllegalStateException {
		return this;
	}
	//
	@Override
	public final void release() {
		POOL.release(this);
	}
}
