package com.emc.mongoose.storage.mock.impl.web.request;
//
//import com.emc.mongoose.common.collections.InstancePool;
//import com.emc.mongoose.common.collections.Reusable;
//import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.http.IOUtils;
import com.emc.mongoose.common.log.LogUtil;
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
	private HttpRequest httpRequest = null;
	//private long expectedContentSize = Constants.BUFF_SIZE_LO;
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
		//expectedContentSize = entity.getContentLength();
	}
	//
	@Override
	protected final void onContentReceived(final ContentDecoder decoder, final IOControl ioCtl) {
		try {
			final long ingestByteCount = IOUtils.consumeQuietly(decoder/*, expectedContentSize*/);
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
