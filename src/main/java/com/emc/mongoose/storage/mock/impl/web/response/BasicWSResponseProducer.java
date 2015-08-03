package com.emc.mongoose.storage.mock.impl.web.response;
// mongoose-common.jar
//import com.emc.mongoose.common.collections.InstancePool;
//import com.emc.mongoose.common.collections.Reusable;
import com.emc.mongoose.common.net.http.content.OutputChannel;
import com.emc.mongoose.common.log.LogUtil;
// mongoose-storage-mock.jar
//
import com.emc.mongoose.storage.mock.api.DataObjectMock;
//
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.protocol.HttpContext;
//
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.HttpAsyncResponseProducer;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
/**
 * Created by olga on 12.02.15.
 */
public final class BasicWSResponseProducer
implements HttpAsyncResponseProducer {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private volatile HttpResponse response = null;
	private final OutputChannel chanOut = new OutputChannel();
	//
	public final void setResponse(final HttpResponse response) {
		this.response = response;
	}
	//
	@Override
	public HttpResponse generateResponse() {
		return this.response;
	}
	//
	@Override
	public final void produceContent(final ContentEncoder encoder, final IOControl ioctrl)
	throws IOException {
		final HttpEntity respEntity = response.getEntity();
		if(DataObjectMock.class.isInstance(respEntity)) {
			chanOut.setContentEncoder(encoder);
			try {
				final DataObjectMock dataItem = DataObjectMock.class.cast(response.getEntity());
				if(dataItem != null) {
					dataItem.writeFully(chanOut);
				}
			} catch(final Exception e) {
				LogUtil.exception(LOG, Level.WARN, e, "Content producing failure");
			} finally {
				try {
					if(!encoder.isCompleted()) {
						encoder.complete();
					}
				} finally {
					chanOut.close();
				}
			}
		} else if(NByteArrayEntity.class.isInstance(respEntity)) {
			((NByteArrayEntity) respEntity).produceContent(encoder, ioctrl);
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
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Reusable implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	/*private final static InstancePool<BasicWSResponseProducer> POOL;
	static {
		InstancePool<BasicWSResponseProducer> t = null;
		try {
			t = new InstancePool<>(BasicWSResponseProducer.class.getConstructor());
		} catch(final NoSuchMethodException e) {
			LogUtil.exception(LOG, Level.FATAL, e, "Failed to create the instance pool");
		}
		POOL = t;
	}
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
	}*/
}
