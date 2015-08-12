package com.emc.mongoose.storage.mock.impl.web.response;
// mongoose-common.jar
import com.emc.mongoose.common.log.Markers;
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
	private DataObjectMock contentDataObject = null;
	private NByteArrayEntity contentBytes = null;
	private long byteCount = 0, contentSize = 0;
	private final OutputChannel chanOut = new OutputChannel();
	//
	public final void setResponse(final HttpResponse response) {
		this.response = response;
		final HttpEntity contentEntity = response.getEntity();
		if(contentEntity != null) {
			contentSize = contentEntity.getContentLength();
			if(DataObjectMock.class.isInstance(contentEntity)) {
				contentDataObject = (DataObjectMock) contentEntity;
				contentBytes = null;
			} else if(NByteArrayEntity.class.isInstance(contentEntity)) {
				contentBytes = (NByteArrayEntity) contentEntity;
				contentDataObject = null;
			}
		}
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
		if(contentDataObject != null) {
			chanOut.setContentEncoder(encoder);
			try {
				contentDataObject.writeFully(chanOut);
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
		} else if(contentBytes != null) {
			contentBytes.produceContent(encoder, ioctrl);
		} else {
			final HttpEntity contentEntity = response.getEntity();
			if(contentEntity != null) {
				LOG.warn(Markers.ERR, "Unsupported content type: " + contentEntity.getClass());
			}
			encoder.complete();
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
