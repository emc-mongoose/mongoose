package com.emc.mongoose.storage.mock.impl.web.request;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtils;
// mongoose-core-api.jar
import static com.emc.mongoose.core.api.io.req.conf.WSRequestConfig.VALUE_RANGE_PREFIX;
import static com.emc.mongoose.core.api.io.req.conf.WSRequestConfig.VALUE_RANGE_CONCAT;
// mongoose-storage-mock.jar
import com.emc.mongoose.storage.mock.api.ContainerMockAlreadyExistsException;
import com.emc.mongoose.storage.mock.api.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.IOStats;
import com.emc.mongoose.storage.mock.api.ObjectMockNotFoundException;
import com.emc.mongoose.storage.mock.api.ReqURIMatchingHandler;
import com.emc.mongoose.storage.mock.api.WSMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
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
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by andrey on 13.05.15.
 */
public abstract class WSRequestHandlerBase<T extends WSObjectMock>
implements ReqURIMatchingHandler<T> {
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
	//
	protected final WSMock<T> sharedStorage;
	protected final int batchSize;
	//
	protected WSRequestHandlerBase(
		final RunTimeConfig runTimeConfig, final WSMock<T> sharedStorage
	) {
		this.rateLimit = runTimeConfig.getLoadLimitRate();
		this.batchSize = runTimeConfig.getBatchSize();
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
		final RequestLine reqLine = httpRequest.getRequestLine();
		//
		handleActually(httpRequest, httpResponse, reqLine.getMethod(), reqLine.getUri());
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
	protected static String randomString(final int len) {
		final byte buff[] = new byte[len];
		ThreadLocalRandom.current().nextBytes(buff);
		return Hex.encodeHexString(buff);
	}
	//
	protected void handleGenericDataReq(
		final HttpRequest httpRequest, final HttpResponse httpResponse,
		final String method, final String container, final String dataId
	) {
		switch(method) {
			case METHOD_POST:
				LOG.info(Markers.MSG, "Write date object request: /{}/{}", container, dataId);
				handleWrite(httpRequest, httpResponse, container, dataId);
				break;
			case METHOD_PUT:
				LOG.info(Markers.MSG, "Write data object request: /{}/{}", container, dataId);
				handleWrite(httpRequest, httpResponse, container, dataId);
				break;
			case METHOD_GET:
				LOG.info(Markers.MSG, "Read data object request: /{}/{}", container, dataId);
				handleRead(httpResponse, container, dataId);
				break;
			case METHOD_HEAD:
				httpResponse.setStatusCode(HttpStatus.SC_OK);
				break;
			case METHOD_DELETE:
				LOG.info(Markers.MSG, "Delete data object request: /{}/{}", container, dataId);
				handleDelete(httpResponse, container, dataId);
				break;
		}
	}
	//
	private void handleWrite(
		final HttpRequest request, final HttpResponse response,
		final String container, final String dataId
	) {
		try {
			response.setStatusCode(HttpStatus.SC_OK);
			final Header rangeHeaders[] = request.getHeaders(HttpHeaders.RANGE);
			//
			if(rangeHeaders == null || rangeHeaders.length == 0) {
				// write or recreate data item
				final T dataObject = createDataObject(request, response, container, dataId);
				ioStats.markCreate(dataObject.getSize());
			} else {
				// else do append or update if data item exist
				handleRanges(
					container, dataId, rangeHeaders,
					HttpEntityEnclosingRequest.class.cast(request).getEntity().getContentLength()
				);
			}
		} catch(final ContainerMockNotFoundException | ObjectMockNotFoundException e) {
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
			ioStats.markCreate(-1);
		} catch(final NumberFormatException | HttpException e) {
			response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			LogUtil.exception(
				LOG, Level.ERROR, e,
				"Failed to decode the data id \"{}\" as ring buffer offset", dataId
			);
			ioStats.markCreate(-1);
		}
	}
	//
	private void handleRanges(
		final String container, final String dataId,
		final Header rangeHeaders[], final long contentLength
	) throws ContainerMockNotFoundException, ObjectMockNotFoundException {
		String rangeHeaderValue, rangeValuePairs[], rangeValue[];
		long offset;
		for(final Header rangeHeader : rangeHeaders) {
			rangeHeaderValue = rangeHeader.getValue();
			if(rangeHeaderValue.startsWith(VALUE_RANGE_PREFIX)) {
				rangeHeaderValue = rangeHeaderValue.substring(
					VALUE_RANGE_PREFIX.length(), rangeHeaderValue.length()
				);
				rangeValuePairs = rangeHeaderValue.split(RunTimeConfig.LIST_SEP);
				for(final String rangeValuePair : rangeValuePairs) {
					rangeValue = rangeValuePair.split(VALUE_RANGE_CONCAT);
					if(rangeValue.length == 1) {
						sharedStorage.append(
							container, dataId, Long.parseLong(rangeValue[0]), contentLength
						);
					} else if(rangeValue.length == 2) {
						offset = Long.parseLong(rangeValue[0]);
						sharedStorage.update(
							container, dataId, offset, Long.parseLong(rangeValue[1]) - offset + 1
						);
					} else {
						LOG.warn(
							Markers.ERR, "Invalid range header value: \"{}\"", rangeHeaderValue
						);
					}
				}
			} else {
				LOG.warn(Markers.ERR, "Invalid range header value: \"{}\"", rangeHeaderValue);
			}
		}
	}
	//
	private void handleRead(
		final HttpResponse response, final String container, final String dataId
	) {
		try {
			final T dataObject = sharedStorage.read(container, dataId, 0, 0);
			response.setStatusCode(HttpStatus.SC_OK);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "Send data object with ID: {}", dataId);
			}
			response.setEntity(dataObject);
			ioStats.markRead(dataObject.getSize());
		} catch(final ContainerMockNotFoundException e) {
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.ERR, "No such container: {}", dataId);
			}
			ioStats.markRead(-1);
		} catch(final ObjectMockNotFoundException e) {
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.ERR, "No such object: {}", dataId);
			}
			ioStats.markRead(-1);
		}
	}
	//
	private void handleDelete(
		final HttpResponse response, final String container, final String dataId
	) {
		try {
			sharedStorage.delete(container, dataId);
			response.setStatusCode(HttpStatus.SC_OK);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "Delete data object with ID: {}", dataId);
			}
			ioStats.markDelete();
		} catch(final ContainerMockNotFoundException e) {
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.ERR, "No such container: {}", dataId);
			}
		} catch(final ObjectMockNotFoundException e) {
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.ERR, "No such object: {}", dataId);
			}
		}
	}
	//
	private T createDataObject(
		final HttpRequest request, final HttpResponse response,
		final String container, final String dataId
	) throws HttpException, NumberFormatException {
		final HttpEntity entity = HttpEntityEnclosingRequest.class.cast(request).getEntity();
		final long size = entity.getContentLength();
		// create data object or get it for append or update
		final long offset = Long.valueOf(dataId, Character.MAX_RADIX);
		T dataObject = null;
		try {
			dataObject = sharedStorage.create(container, dataId, offset, size);
		} catch(final ContainerMockNotFoundException e) {
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
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
	protected void handleGenericContainerReq(
		final HttpRequest httpRequest, final HttpResponse httpResponse,
		final String method, final String container, final String dataId
	) {
		switch(method) {
			case METHOD_POST:
				httpResponse.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
				break;
			case METHOD_PUT:
				LOG.info(Markers.MSG, "Create container request: {}", container);
				handleContainerCreate(httpRequest, httpResponse, container);
				break;
			case METHOD_GET:
				LOG.info(Markers.MSG, "List container request: {}/{}", container, dataId);
				handleContainerList(httpRequest, httpResponse, container, dataId);
				break;
			case METHOD_HEAD:
				LOG.info(Markers.MSG, "Check container existence request: {}", container);
				handleContainerExists(httpResponse, container);
				break;
			case METHOD_DELETE:
				LOG.info(Markers.MSG, "Delete container request: {}", container);
				handleContainerDelete(httpResponse, container);
				break;
		}
	}
	//
	private void handleContainerCreate(
		final HttpRequest req, final HttpResponse resp, final String name
	) {
		try {
			sharedStorage.create(name);
		} catch(final ContainerMockAlreadyExistsException e) {
			resp.setStatusCode(HttpStatus.SC_CONFLICT);
		}
	}
	//
	protected abstract void handleContainerList(
		final HttpRequest req, final HttpResponse resp, final String name, final String dataId
	);
	//
	private void handleContainerExists(final HttpResponse resp, final String name) {
		if(!sharedStorage.exists(name)) {
			resp.setStatusCode(HttpStatus.SC_NOT_FOUND);
		}
	}
	//
	private void handleContainerDelete(final HttpResponse resp, final String name) {
		try {
			sharedStorage.delete(name);
		} catch(final ContainerMockNotFoundException e) {
			resp.setStatusCode(HttpStatus.SC_NOT_FOUND);
		}
	}
}
