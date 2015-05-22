package com.emc.mongoose.storage.mock.impl.cinderella.response;
//
import com.emc.mongoose.common.collections.InstancePool;
import com.emc.mongoose.common.collections.Reusable;
import com.emc.mongoose.common.io.HTTPContentEncoderChannel;
import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.storage.mock.api.data.WSObjectMock;
import org.apache.http.HttpResponse;
//
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
//
import org.apache.http.nio.protocol.HttpAsyncResponseProducer;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
/**
 * Created by olga on 12.02.15.
 */
public final class BasicWSResponseProducer
implements HttpAsyncResponseProducer, Reusable<BasicWSResponseProducer> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private volatile HttpResponse response = null;
	//
	@Override
	public HttpResponse generateResponse() {
		return this.response;
	}
	//
	@Override
	public final void produceContent(final ContentEncoder encoder, final IOControl ioctrl)
	throws IOException {
		try(final WritableByteChannel chanOut = HTTPContentEncoderChannel.getInstance(encoder)) {
			final WSObjectMock dataItem = WSObjectMock.class.cast(response.getEntity());
			if(dataItem != null) {
				if(LOG.isTraceEnabled(LogUtil.MSG)) {
					LOG.trace(LogUtil.MSG, "{}: write out {} bytes", dataItem, dataItem.getSize());
				}
				dataItem.write(chanOut);
			}
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Content producing failure");
		}
	}
	//
	@Override
	public final void responseCompleted(final HttpContext context) {
	}
	//
	@Override
	public final void failed(final Exception e) {
		LogUtil.exception(LOG, Level.WARN, e, "Response failure");
	}
	//
	@Override
	public final void close()
	throws IOException {
		release();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Reusable implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static InstancePool<BasicWSResponseProducer> POOL = new InstancePool<>(
		BasicWSResponseProducer.class
	);
	//
	public static BasicWSResponseProducer getInstance(final HttpResponse response) {
		return POOL.take(response);
	}
	//
	@Override
	public final Reusable<BasicWSResponseProducer> reuse(final Object... args)
	throws IllegalArgumentException, IllegalStateException {
		if(args != null) {
			if(args.length > 0) {
				response = HttpResponse.class.cast(args[0]);
			}
		}
		return this;
	}
	//
	@Override
	public final void release() {
		POOL.release(this);
	}
}
