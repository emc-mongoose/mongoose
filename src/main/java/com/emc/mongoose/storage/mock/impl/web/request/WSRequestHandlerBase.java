package com.emc.mongoose.storage.mock.impl.web.request;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtils;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataObject;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.data.UniformData;
// mongoose-storage-mock.jar
import com.emc.mongoose.storage.mock.api.ObjectStorage;
import com.emc.mongoose.storage.mock.api.IOStats;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
import com.emc.mongoose.storage.mock.impl.web.data.BasicWSObjectMock;
import com.emc.mongoose.storage.mock.impl.web.response.BasicWSResponseProducer;
//
import org.apache.commons.codec.binary.Hex;
//
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
	//private final static int RING_OFFSET_RADIX = RunTimeConfig.getContext().getDataRadixOffset();
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
	private final ObjectStorage<T> sharedStorage;
	//
	protected WSRequestHandlerBase(
		final RunTimeConfig runTimeConfig, final ObjectStorage<T> sharedStorage
	) {
		this.rateLimit = runTimeConfig.getLoadLimitRate();
		this.sharedStorage = sharedStorage;
		this.ioStats = sharedStorage.getStats();
	}
	//
	private final static ThreadLocal<BasicWSRequestConsumer>
		THRLOC_REQ_CONSUMER = new ThreadLocal<>();
	@Override
	public final HttpAsyncRequestConsumer<HttpRequest> processRequest(
		final HttpRequest request, final HttpContext context
	) throws HttpException, IOException {
		try {
			BasicWSRequestConsumer reqConsumer = THRLOC_REQ_CONSUMER.get();
			if(reqConsumer == null) {
				reqConsumer = new BasicWSRequestConsumer();
				THRLOC_REQ_CONSUMER.set(reqConsumer);
			}
			return reqConsumer;
		} catch(final IllegalArgumentException | IllegalStateException e) {
			throw new MethodNotSupportedException("Request consumer instantiation failure", e);
		}
	}
	//
	private final static ThreadLocal<BasicWSResponseProducer>
		THRLOC_RESP_PRODUCER = new ThreadLocal<>();
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
		BasicWSResponseProducer respProducer = THRLOC_RESP_PRODUCER.get();
		if(respProducer == null) {
			respProducer = new BasicWSResponseProducer();
			THRLOC_RESP_PRODUCER.set(respProducer);
		}
		respProducer.setResponse(httpResponse);
		httpExchange.submitResponse(respProducer);
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
				handleWrite(httpRequest, httpResponse, dataId);
				break;
			case METHOD_PUT:
				handleWrite(httpRequest, httpResponse, dataId);
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
	private void handleWrite(
		final HttpRequest httpRequest, final HttpResponse httpResponse, final String dataId
	) {
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, "Create data object with ID: {}", dataId);
		}
		try {
			httpResponse.setStatusCode(HttpStatus.SC_OK);
			T dataObject;
			final Header rangeHeaders[] = httpRequest.getHeaders(HttpHeaders.RANGE);
			//
			if(rangeHeaders == null || rangeHeaders.length == 0) {
				// write or recreate data item
				dataObject = createDataObject(httpRequest, dataId);
				ioStats.markCreate(dataObject.getSize());
			} else if(sharedStorage.get(dataId) != null) {
				// else do append or update if data item exist
				dataObject = sharedStorage.get(dataId);
				handleRanges(
					dataObject, rangeHeaders, httpResponse,
					HttpEntityEnclosingRequest.class.cast(httpRequest).getEntity().getContentLength()
				);
			} else {
				httpResponse.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				LOG.debug(Markers.ERR, "Unknown request");
			}
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
	private final static String
		KEY_RANGE_START = "rangeStart",
		KEY_RANGE_END = "rangeEnd";
	private final static Pattern PATTERN_RANGE = Pattern.compile(
		"bytes=((?<" + KEY_RANGE_START + ">\\d+)\\-(?<" + KEY_RANGE_END + ">\\d*),?)+"
	);
	//
	private void handleRanges(
		final T dataObject, final Header rangeHeaders[], final HttpResponse httpResponse,
	    final long contentLength
	) {
		String rangeHeaderValue, rangeStartValue, rangeEndValue;
		long rangeStart, rangeEnd;
		for(final Header rangeHeader : rangeHeaders) {
			rangeHeaderValue = rangeHeader.getValue();
			try {
				final Matcher matcher = PATTERN_RANGE.matcher(rangeHeader.getValue());
				while(matcher.find()) {
					rangeStartValue = matcher.group(KEY_RANGE_START);
					rangeStart = Long.parseLong(rangeStartValue);
					rangeEndValue = matcher.group(KEY_RANGE_END);
					if(rangeEndValue == null || rangeEndValue.length() == 0) {
						sharedStorage.append(dataObject, rangeStart, contentLength);
						break;
					} else {
						rangeEnd = Long.parseLong(rangeEndValue);
						sharedStorage.update(dataObject, rangeStart, rangeEnd);
					}
				}
			} catch(final Exception e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to parse the range header: \"{}\"", rangeHeader
				);
			}
		}
		if(LOG.isTraceEnabled(Markers.DATA_LIST)) {
			LOG.trace(Markers.DATA_LIST, dataObject);
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
	private T createDataObject(final HttpRequest request, final String dataID)
	throws HttpException, NumberFormatException {
		final HttpEntity entity = HttpEntityEnclosingRequest.class.cast(request).getEntity();
		final long bytes = entity.getContentLength();
		// create data object or get it for append or update
		final long offset = Long.valueOf(dataID, Character.MAX_RADIX);
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
	offset for mongoose versions prior to v0.4:
		final long offset = Long.valueOf(dataID, 0x10);
	@Deprecated
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
	}*/
	//
	protected static String generateId() {
		return Long.toString(UniformData.nextOffset(LAST_OFFSET), DataObject.ID_RADIX);
	}
}
