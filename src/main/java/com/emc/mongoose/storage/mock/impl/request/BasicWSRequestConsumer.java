package com.emc.mongoose.storage.mock.impl.request;
//
//import com.emc.mongoose.common.collections.InstancePool;
//import com.emc.mongoose.common.collections.Reusable;
import com.emc.mongoose.common.io.StreamUtils;
import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.common.logging.Markers;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import org.apache.http.protocol.HttpContext;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.entity.ContentType;
//
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.AbstractAsyncRequestConsumer;
//
import java.io.IOException;
import java.nio.ByteBuffer;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 * Created by olga on 04.02.15.
 */
public final class BasicWSRequestConsumer
extends AbstractAsyncRequestConsumer<HttpRequest> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private ByteBuffer bbuff = null;
	private HttpRequest httpRequest = null;
	//
	public BasicWSRequestConsumer() {
		super();
	}
	//
	@Override
	protected final void onRequestReceived(final HttpRequest httpRequest)
	throws HttpException, IOException {
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, "Got request: {}", httpRequest);
		}
		this.httpRequest = httpRequest;
	}
	//
	@Override
	protected final void onEntityEnclosed(final HttpEntity entity, final ContentType contentType) {
		final long dataSize = entity.getContentLength();
		// adapt the buffer size or reuse existing thread local buffer if any
		if(dataSize > LoadExecutor.BUFF_SIZE_HI) {
			if(bbuff == null || bbuff.capacity() != LoadExecutor.BUFF_SIZE_HI) {
				bbuff = ByteBuffer.allocate(LoadExecutor.BUFF_SIZE_HI);
			}
		} else if(dataSize < LoadExecutor.BUFF_SIZE_LO) {
			if(bbuff == null || bbuff.capacity() != LoadExecutor.BUFF_SIZE_LO) {
				bbuff = ByteBuffer.allocate(LoadExecutor.BUFF_SIZE_LO);
			}
		} else {
			// reallocate only if content length is twice less/more than buffer size
			if(bbuff == null || dataSize > 2 * bbuff.capacity() || bbuff.capacity() > 2 * dataSize) {
				bbuff = ByteBuffer.allocate((int) dataSize); // type cast should be safe here
			}
		}
	}
	//
	@Override
	protected final void onContentReceived(final ContentDecoder decoder, final IOControl ioCtl) {
		try {
			bbuff.clear();
			final long ingestByteCount = StreamUtils.consumeQuietly(decoder, ioCtl, bbuff);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "Consumed {} bytes", ingestByteCount);
			}
		} catch(final Throwable e) {
			LogUtil.exception(LOG, Level.WARN, e, "Content consuming failure");
		}
	}
	@Override
	protected final HttpRequest buildResult(final HttpContext context)
	throws Exception {
		return httpRequest;
	}
	@Override
	protected void releaseResources() {
		httpRequest = null;
		//release();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Reusable implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	/*private final static InstancePool<BasicWSRequestConsumer> POOL;
	static {
		InstancePool<BasicWSRequestConsumer> t = null;
		try {
			t = new InstancePool<>(BasicWSRequestConsumer.class.getConstructor());
		} catch(final NoSuchMethodException e) {
			LogUtil.exception(LOG, Level.FATAL, e, "Failed to create the instance pool");
		}
		POOL = t;
	}
	//
	public static BasicWSRequestConsumer getInstance()
	throws IllegalStateException, IllegalArgumentException {
		return POOL.take();
	}
	//
	@Override
	public final Reusable<BasicWSRequestConsumer> reuse(final Object... args)
	throws IllegalArgumentException, IllegalStateException {
		return this;
	}
	//
	@Override
	public final void release() {
		POOL.release(this);
	}*/
}
