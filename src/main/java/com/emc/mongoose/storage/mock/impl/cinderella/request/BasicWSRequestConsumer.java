package com.emc.mongoose.storage.mock.impl.cinderella.request;
//
import com.emc.mongoose.common.collections.InstancePool;
import com.emc.mongoose.common.collections.Reusable;
import com.emc.mongoose.common.io.StreamUtils;
import com.emc.mongoose.common.logging.LogUtil;
//
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
extends AbstractAsyncRequestConsumer<HttpRequest>
implements Reusable<BasicWSRequestConsumer> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static ThreadLocal<ByteBuffer> THRLOC_BB = new ThreadLocal<>();
	//
	private ByteBuffer bbuff = null;
	private HttpRequest httpRequest = null;
	//
	public BasicWSRequestConsumer() {
		super();
	}
	@Override
	protected final void onRequestReceived(final HttpRequest httpRequest)
	throws HttpException, IOException {
		this.httpRequest = httpRequest;
	}
	//
	@Override
	protected final void onEntityEnclosed(final HttpEntity entity, final ContentType contentType) {
		final long dataSize = entity.getContentLength();
		// adapt the buffer size or reuse existing thread local buffer if any
		bbuff = THRLOC_BB.get();
		if(dataSize > LoadExecutor.BUFF_SIZE_HI) {
			if(bbuff == null || bbuff.capacity() != LoadExecutor.BUFF_SIZE_HI) {
				bbuff = ByteBuffer.allocate(LoadExecutor.BUFF_SIZE_HI);
				THRLOC_BB.set(bbuff);
			}
		} else if(dataSize < LoadExecutor.BUFF_SIZE_LO) {
			if(bbuff == null || bbuff.capacity() != LoadExecutor.BUFF_SIZE_LO) {
				bbuff = ByteBuffer.allocate(LoadExecutor.BUFF_SIZE_LO);
				THRLOC_BB.set(bbuff);
			}
		} else {
			// reallocate only if content length is twice less/more than buffer size
			if(bbuff == null || dataSize > 2 * bbuff.capacity() || bbuff.capacity() > 2 * dataSize) {
				bbuff = ByteBuffer.allocate((int) dataSize); // type cast should be safe here
				THRLOC_BB.set(bbuff);
			}
		}
	}
	//
	@Override
	protected final void onContentReceived(final ContentDecoder decoder, final IOControl ioCtl) {
		try {
			bbuff.clear();
			StreamUtils.consumeQuietly(decoder, ioCtl, bbuff);
		} catch(final Throwable e) {
			LogUtil.failure(LOG, Level.WARN, e, "Content consuming failure");
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
		release();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Reusable implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static InstancePool<BasicWSRequestConsumer> POOL = new InstancePool<>(
		BasicWSRequestConsumer.class
	);
	//
	public static BasicWSRequestConsumer getInstance() {
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
	}
}
