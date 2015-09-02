package com.emc.mongoose.storage.mock.impl.web.response;
// mongoose-common.jar
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.http.content.OutputChannel;
import com.emc.mongoose.common.log.LogUtil;
// mongoose-storage-mock.jar
//
import com.emc.mongoose.core.impl.data.RangeLayerData;
//
import com.emc.mongoose.core.impl.data.UniformData;
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
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
	private WSObjectMock contentDataObject = null;
	private NByteArrayEntity contentBytes = null;
	private long byteCount = 0, contentSize = 0, rangeSize = 0;
	private OutputChannel chanOut = null;
	private int rangeIdx = 0;
	private UniformData currRange = null;
	//
	public final void setResponse(final HttpResponse response) {
		this.response = response;
		final HttpEntity contentEntity = response.getEntity();
		if(contentEntity != null) {
			contentSize = contentEntity.getContentLength();
			if(contentEntity instanceof WSObjectMock) {
				contentDataObject = (WSObjectMock) contentEntity;
				contentBytes = null;
			} else if(contentEntity instanceof NByteArrayEntity) {
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
			produceDataObjectContent(encoder);
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
	private void produceDataObjectContent(final ContentEncoder encoder)
	throws IOException {
		if(chanOut == null) {
			chanOut = new OutputChannel(encoder);
		}
		try {
			if(!contentDataObject.hasAnyUpdatedRanges()) {
				if(byteCount == contentSize) {
					chanOut.close();
				}
				byteCount += contentDataObject.write(chanOut, contentSize - byteCount);
			} else {
				if(byteCount == rangeSize) {
					rangeSize = contentDataObject.getRangeSize(rangeIdx);
					currRange = new UniformData(
						contentDataObject.getOffset() + RangeLayerData.getRangeOffset(rangeIdx),
						rangeSize,
						contentDataObject.isCurrLayerRangeUpdating(rangeIdx) ?
							contentDataObject.getCurrLayerIndex() + 1 :
							contentDataObject.getCurrLayerIndex(),
						UniformDataSource.DEFAULT
					);
					rangeIdx ++;
				}
				if(rangeSize > 0) {
					byteCount += currRange.write(chanOut, rangeSize - byteCount);
				} else {
					chanOut.close();
				}
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
