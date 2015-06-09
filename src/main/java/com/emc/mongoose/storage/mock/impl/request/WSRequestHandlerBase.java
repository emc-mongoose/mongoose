package com.emc.mongoose.storage.mock.impl.request;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.logging.Markers;
import com.emc.mongoose.common.net.ServiceUtils;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataObject;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.data.UniformData;
// mongoose-storage-mock.jar
import com.emc.mongoose.storage.mock.api.Storage;
import com.emc.mongoose.storage.mock.api.data.WSObjectMock;
import com.emc.mongoose.storage.mock.api.stats.IOStats;
import com.emc.mongoose.storage.mock.impl.response.BasicWSResponseProducer;
import com.emc.mongoose.storage.mock.impl.data.BasicWSObjectMock;
//
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
//
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.RequestLine;
import org.apache.http.protocol.HttpContext;
//
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by andrey on 13.05.15.
 */
public abstract class WSRequestHandlerBase<T extends WSObjectMock>
implements HttpAsyncRequestHandler<HttpRequest> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final static String
		METHOD_PUT = "put",
		METHOD_GET = "get",
		METHOD_POST = "post",
		METHOD_HEAD = "head",
		METHOD_DELETE = "delete",
		METHOD_TRACE = "trace";
	//
	private final static int RING_OFFSET_RADIX = RunTimeConfig.getContext().getDataRadixOffset();
	private final static AtomicLong
		LAST_OFFSET = new AtomicLong(
			Math.abs(
				Long.reverse(System.currentTimeMillis()) ^
					Long.reverseBytes(System.nanoTime()) ^
					ServiceUtils.getHostAddrCode()
			)
		);
	private final IOStats ioStats;
	private final float rateLimit;
	private final AtomicInteger lastMilliDelay = new AtomicInteger(1);
	private final Storage<T> sharedStorage;
	//
	protected WSRequestHandlerBase(
		final RunTimeConfig runTimeConfig, final Storage<T> sharedStorage
	) {
		this.rateLimit = runTimeConfig.getLoadLimitRate();
		this.sharedStorage = sharedStorage;
		this.ioStats = sharedStorage.getStats();
	}
	//
	@Override
	public final HttpAsyncRequestConsumer<HttpRequest> processRequest(
		final HttpRequest request, final HttpContext context
	) throws HttpException, IOException {
		try {
			return BasicWSRequestConsumer.getInstance();
		} catch(final IllegalArgumentException | IllegalStateException e) {
			throw new MethodNotSupportedException("Request consumer instantiation failure", e);
		}
	}
	//
	@Override
	public final void handle(
		final HttpRequest r, final HttpAsyncExchange httpExchange, final HttpContext httpContext
	) {
		// load rate limitation algorithm
		if(rateLimit > 0) {
			if(ioStats.getMeanRate() > rateLimit) {
				try {
					Thread.sleep(lastMilliDelay.incrementAndGet());
				} catch(final InterruptedException e) {
					return;
				}
			} else if(lastMilliDelay.get() > 0) {
				lastMilliDelay.decrementAndGet();
			}
		}
		// prepare
		final HttpRequest httpRequest = httpExchange.getRequest();
		final HttpResponse httpResponse = httpExchange.getResponse();
		final RequestLine requestLine = httpRequest.getRequestLine();
		final String method = requestLine.getMethod().toLowerCase(LogUtil.LOCALE_DEFAULT);
		// get URI components
		final String[] requestURI = requestLine.getUri().split("/");
		final String dataId = requestURI[requestURI.length - 1];
		//
		handleActually(httpRequest, httpResponse, method, requestURI, dataId);
		// done
		httpExchange.submitResponse(BasicWSResponseProducer.getInstance(httpResponse));
	}
	//
	protected abstract void handleActually(
		final HttpRequest httpRequest, final HttpResponse httpResponse,
		final String method, final String requestURI[], final String dataId
	);
	//
	protected static String randomString(final int len) {
		final byte buff[] = new byte[len];
		ThreadLocalRandom.current().nextBytes(buff);
		return Hex.encodeHexString(buff);
	}
	//
	protected void handleGenericDataReq(
		final HttpRequest httpRequest, final HttpResponse httpResponse, final String method,
		final String dataId
	) {
		switch(method) {
			case METHOD_POST:
				handleCreate(httpRequest, httpResponse, dataId);
				break;
			case METHOD_PUT:
				handleCreate(httpRequest, httpResponse, dataId);
				break;
			case METHOD_GET:
				handleRead(httpResponse, dataId);
				break;
			case METHOD_HEAD:
				httpResponse.setStatusCode(HttpStatus.SC_OK);
				break;
			case METHOD_DELETE:
				handleDelete(httpResponse, dataId);
				break;
	}
	}
	//
	private void handleCreate(
		final HttpRequest httpRequest, final HttpResponse httpResponse, final String dataId
	) {
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, "Create data object with ID: {}", dataId);
		}
		try {
			httpResponse.setStatusCode(HttpStatus.SC_OK);
			final WSObjectMock dataObject = writeDataObject(httpRequest, dataId);
			ioStats.markCreate(dataObject.getSize());
		} catch(final HttpException e) {
			httpResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			LogUtil.exception(LOG, Level.ERROR, e, "Put method failure");
			ioStats.markCreate(-1);
		} catch(final NumberFormatException e){
			httpResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			LogUtil.exception(
				LOG, Level.ERROR, e,
				"Failed to decode the data id \"{}\" as ring buffer offset", dataId
			);
			ioStats.markCreate(-1);
		}
	}
	//
	private void handleRead(final HttpResponse response, final String dataId) {
		final WSObjectMock dataObject = sharedStorage.get(dataId);
		if(dataObject == null) {
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.ERR, "No such object: {}", dataId);
			}
			ioStats.markRead(-1);
		} else {
			response.setStatusCode(HttpStatus.SC_OK);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "Send data object with ID: {}", dataId);
			}
			response.setEntity(dataObject);
			ioStats.markRead(dataObject.getSize());
		}
	}
	//
	private void handleDelete(final HttpResponse response, final String dataId){
		final T dataObject = sharedStorage.get(dataId);
		if(dataObject == null) {
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.ERR, "No such object: {}", dataId);
			}
		} else {
			sharedStorage.delete(dataObject);
			response.setStatusCode(HttpStatus.SC_OK);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "Delete data object with ID: {}", dataId);
			}
			ioStats.markDelete();
		}
	}
	//
	private WSObjectMock writeDataObject(final HttpRequest request, final String dataID)
	throws HttpException, NumberFormatException {
		final HttpEntity entity = HttpEntityEnclosingRequest.class.cast(request).getEntity();
		final long bytes = entity.getContentLength();
		// create data object or get it for append or update
		final long offset = decodeRingBufferOffset(dataID);
		final T dataObject = (T) new BasicWSObjectMock(dataID, offset, bytes);
		sharedStorage.create(dataObject);
		if(LOG.isTraceEnabled(Markers.DATA_LIST)) {
			LOG.trace(Markers.DATA_LIST, dataObject);
		}
		return dataObject;
	}
	/*
	offset for mongoose versions since v0.6:
		final long offset = Long.valueOf(dataID, WSRequestConfigBase.RADIX);
	offset for mongoose v0.4x and 0.5x:
		final byte dataIdBytes[] = Base64.decodeBase64(dataID);
		final long offset  = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).put(dataIdBytes).getLong(0);
	offset for mongoose versions prior to v.0.4:
		final long offset = Long.valueOf(dataID, 0x10);
	 */
	private static long decodeRingBufferOffset(final String dataID)
	throws HttpException, NumberFormatException {
		long offset;
		if(RING_OFFSET_RADIX == 0x40) { // base64
			offset = ByteBuffer
				.allocate(Long.SIZE / Byte.SIZE)
				.put(Base64.decodeBase64(dataID))
				.getLong(0);
		} else if(RING_OFFSET_RADIX > 1 && RING_OFFSET_RADIX <= Character.MAX_RADIX) {
			offset = Long.valueOf(dataID, RING_OFFSET_RADIX);
		} else {
			throw new HttpException("Unsupported data ring offset radix: " + RING_OFFSET_RADIX);
		}
		return offset;
	}
	//
	protected static String generateId() {
		return Long.toString(UniformData.nextOffset(LAST_OFFSET), DataObject.ID_RADIX);
	}
}
