package com.emc.mongoose.storage.mock.impl.web.response;
// mongoose-common.jar
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.http.content.OutputChannel;
import com.emc.mongoose.common.log.LogUtil;
// mongoose-storage-mock.jar
//
import com.emc.mongoose.core.api.item.data.ContentSource;
import com.emc.mongoose.core.impl.item.data.BasicMutableDataItem;
//
import com.emc.mongoose.core.impl.item.data.BasicDataItem;
//
import com.emc.mongoose.storage.mock.api.HttpDataItemMock;
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
	private volatile OutputChannel chanOut = null;
	//
	private volatile HttpDataItemMock dataObject = null;
	private volatile NByteArrayEntity contentBytes = null;
	//
	private volatile long countBytesDone = 0, contentSize = 0;
	//
	private volatile BasicDataItem currRange = null;
	private volatile long currRangeSize = 0, nextRangeOffset = 0;
	private volatile int currRangeIdx = 0, currDataLayerIdx = 0;
	//
	private final ContentSource contentSrc;
	//
	public BasicWSResponseProducer(final ContentSource contentSrc) {
		this.contentSrc = contentSrc;
	}
	//
	public final void setResponse(final HttpResponse response) {
		this.response = response;
		final HttpEntity contentEntity = response.getEntity();
		if(contentEntity != null) {
			contentSize = contentEntity.getContentLength();
			if(contentEntity instanceof HttpDataItemMock) {
				dataObject = (HttpDataItemMock) contentEntity;
				dataObject.reset();
				currDataLayerIdx = dataObject.getCurrLayerIndex();
				contentBytes = null;
			} else if(contentEntity instanceof NByteArrayEntity) {
				contentBytes = (NByteArrayEntity) contentEntity;
				dataObject = null;
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
		if(dataObject != null) {
			produceObjectContent(encoder);
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
	private void produceObjectContent(final ContentEncoder encoder)
	throws IOException {
		//
		if(chanOut == null) { // 1st time invocation
			if(contentSize == 0) { // nothing to do
				encoder.complete();
				return;
			} else { // wrap the encoder w/ output channel
				chanOut = new OutputChannel(encoder);
			}
		}
		//
		try {
			if(dataObject.hasBeenUpdated()) {
				produceUpdatedObjectContent();
			} else {
				produceNotUpdatedObjectContent();
			}
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Content producing failure");
		}
	}
	//
	private void produceNotUpdatedObjectContent()
	throws IOException {
		countBytesDone += dataObject.write(chanOut, contentSize - countBytesDone);
		if(countBytesDone == contentSize) {
			chanOut.close();
		}
	}
	//
	private void produceUpdatedObjectContent()
	throws IOException {
		if(countBytesDone == nextRangeOffset) {
			currRangeSize = dataObject.getRangeSize(currRangeIdx);
			if(dataObject.isCurrLayerRangeUpdated(currRangeIdx)) {
				currRange = new BasicDataItem(
					dataObject.getOffset() + nextRangeOffset, currRangeSize, currDataLayerIdx + 1,
					contentSrc
				);
			} else {
				currRange = new BasicDataItem(
					dataObject.getOffset() + nextRangeOffset, currRangeSize, currDataLayerIdx,
					contentSrc
				);
			}
			currRangeIdx ++;
			nextRangeOffset = BasicMutableDataItem.getRangeOffset(currRangeIdx);
		}
		if(currRangeSize > 0) {
			countBytesDone += currRange.write(
				chanOut, nextRangeOffset - countBytesDone
			);
			if(countBytesDone == contentSize) {
				chanOut.close();
			}
		} else {
			chanOut.close();
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
