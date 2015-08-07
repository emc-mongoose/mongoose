package com.emc.mongoose.storage.mock.impl.web.request;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import static com.emc.mongoose.core.api.io.req.WSRequestConfig.VALUE_RANGE_PREFIX;
import static com.emc.mongoose.core.api.io.req.WSRequestConfig.VALUE_RANGE_CONCAT;
// mongoose-storage-mock.jar
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
import com.emc.mongoose.storage.mock.api.ContainerMockException;
import com.emc.mongoose.storage.mock.api.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.StorageIOStats;
import com.emc.mongoose.storage.mock.api.ObjectMockNotFoundException;
import com.emc.mongoose.storage.mock.api.ReqURIMatchingHandler;
import com.emc.mongoose.storage.mock.api.StorageMockCapacityLimitReachedException;
import com.emc.mongoose.storage.mock.api.WSMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
import com.emc.mongoose.storage.mock.impl.web.response.BasicWSResponseProducer;
//
import org.apache.commons.codec.binary.Hex;
//
import org.apache.http.Header;
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
/**
 Created by andrey on 13.05.15.
 */
public abstract class WSRequestHandlerBase<T extends WSObjectMock>
implements ReqURIMatchingHandler<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final StorageIOStats ioStats;
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
		final HttpRequest req, final HttpAsyncExchange httpExchange, final HttpContext httpContext
	) {
		// load rate limitation algorithm
		if(rateLimit > 0) {
			if(ioStats.getRate() > rateLimit) {
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
		handleActually(
			httpRequest, httpResponse, reqLine.getMethod().toLowerCase(), reqLine.getUri()
		);
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
	/**
	 @param httpRequest
	 @param httpResponse
	 @param method
	 @param container
	 @param oid
	 @param offset note that this is a ring buffer offset only
	 */
	protected void handleGenericDataReq(
		final HttpRequest httpRequest, final HttpResponse httpResponse,
		final String method, final String container, final String oid, final long offset
	) {
		if(container == null) {
			httpResponse.setStatusCode(HttpStatus.SC_BAD_REQUEST);
		} else {
			switch(method.toUpperCase()) {
				case WSRequestConfig.METHOD_POST:
					handleWrite(httpRequest, httpResponse, container, oid, offset);
					break;
				case WSRequestConfig.METHOD_PUT:
					handleWrite(httpRequest, httpResponse, container, oid, offset);
					break;
				case WSRequestConfig.METHOD_GET:
					handleRead(httpResponse, container, oid, offset);
					break;
				case WSRequestConfig.METHOD_HEAD:
					httpResponse.setStatusCode(HttpStatus.SC_OK);
					break;
				case WSRequestConfig.METHOD_DELETE:
					handleDelete(httpResponse, container, oid, offset);
					break;
			}
		}
	}
	//
	private void handleWrite(
		final HttpRequest request, final HttpResponse response,
		final String container, final String oid, final long offset
	) {
		try {
			response.setStatusCode(HttpStatus.SC_OK);
			final long size = ((HttpEntityEnclosingRequest) request)
				.getEntity()
				.getContentLength();
			final Header rangeHeaders[] = request.getHeaders(HttpHeaders.RANGE);
			//
			if(rangeHeaders == null || rangeHeaders.length == 0) {
				// write or rewrite data item
				try {
					sharedStorage.createObject(container, oid, offset, size);
					ioStats.markWrite(true, size);
				} catch(final ContainerMockNotFoundException e) {
					response.setStatusCode(HttpStatus.SC_NOT_FOUND);
					ioStats.markWrite(false, size);
				} catch(final StorageMockCapacityLimitReachedException e) {
					response.setStatusCode(HttpStatus.SC_INSUFFICIENT_STORAGE);
					ioStats.markWrite(false, size);
				}
			} else {
				// else do append or update if data item exist
				ioStats.markWrite(
					handlePartialWrite(container, oid, rangeHeaders, size),
					size
				);
			}
		} catch(final ContainerMockNotFoundException | ObjectMockNotFoundException e) {
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
			ioStats.markWrite(false, 0);
		} catch(final ContainerMockException | NumberFormatException e) {
			response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to perform a range update/append for \"{}\"", oid
			);
			ioStats.markWrite(false, 0);
		}
	}
	//
	private boolean handlePartialWrite(
		final String container, final String dataId,
		final Header rangeHeaders[], final long contentLength
	) throws ContainerMockException, ObjectMockNotFoundException {
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
						sharedStorage.appendObject(
							container, dataId, Long.parseLong(rangeValue[0]), contentLength
						);
					} else if(rangeValue.length == 2) {
						offset = Long.parseLong(rangeValue[0]);
						sharedStorage.updateObject(
							container, dataId, offset, Long.parseLong(rangeValue[1]) - offset + 1
						);
					} else {
						LOG.warn(
							Markers.ERR, "Invalid range header value: \"{}\"", rangeHeaderValue
						);
						return false;
					}
				}
			} else {
				LOG.warn(Markers.ERR, "Invalid range header value: \"{}\"", rangeHeaderValue);
				return false;
			}
		}
		return true;
	}
	//
	private void handleRead(
		final HttpResponse response, final String container, final String dataId, final long offset
	) {
		try {
			final T dataObject = sharedStorage.getObject(container, dataId, offset, 0);
			if(dataObject == null) {
				response.setStatusCode(HttpStatus.SC_NOT_FOUND);
				ioStats.markRead(false, 0);
			} else {
				response.setStatusCode(HttpStatus.SC_OK);
				ioStats.markRead(true, dataObject.getSize());
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(Markers.MSG, "Send data object with ID: {}", dataId);
				}
				response.setEntity(dataObject);
			}
		} catch(final ContainerMockNotFoundException e) {
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
			if(LOG.isTraceEnabled(Markers.ERR)) {
				LOG.trace(Markers.ERR, "No such container: {}", dataId);
			}
			ioStats.markRead(false, 0);
		} catch(final ContainerMockException e) {
			response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			LogUtil.exception(LOG, Level.WARN, e, "Container \"{}\" failure", container);
			ioStats.markRead(false, 0);
		}
	}
	//
	private void handleDelete(
		final HttpResponse response, final String container, final String dataId, final long offset
	) {
		try {
			sharedStorage.deleteObject(container, dataId, offset, -1);
			response.setStatusCode(HttpStatus.SC_OK);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "Delete data object with ID: {}", dataId);
			}
			ioStats.markDelete(true);
		} catch(final ContainerMockNotFoundException e) {
			ioStats.markDelete(false);
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.ERR, "No such container: {}", dataId);
			}
		}
	}
	//
	protected void handleGenericContainerReq(
		final HttpRequest httpRequest, final HttpResponse httpResponse,
		final String method, final String container, final String dataId
	) {
		switch(method.toUpperCase()) {
			case WSRequestConfig.METHOD_POST:
				httpResponse.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
				break;
			case WSRequestConfig.METHOD_PUT:
				handleContainerCreate(httpRequest, httpResponse, container);
				break;
			case WSRequestConfig.METHOD_GET:
				handleContainerList(httpRequest, httpResponse, container, dataId);
				break;
			case WSRequestConfig.METHOD_HEAD:
				handleContainerExists(httpResponse, container);
				break;
			case WSRequestConfig.METHOD_DELETE:
				handleContainerDelete(httpResponse, container);
				break;
		}
	}
	//
	protected boolean handleContainerCreate(
		final HttpRequest req, final HttpResponse resp, final String name
	) {
		try {
			if(!sharedStorage.createContainer(name)) {
				resp.setStatusCode(HttpStatus.SC_CONFLICT);
				return false;
			}
		} catch(final StorageMockCapacityLimitReachedException e) {
			resp.setStatusCode(HttpStatus.SC_INSUFFICIENT_STORAGE);
			return false;
		}
		return true;
	}
	//
	protected abstract void handleContainerList(
		final HttpRequest req, final HttpResponse resp, final String name, final String dataId
	);
	//
	private void handleContainerExists(final HttpResponse resp, final String name) {
		if(null == sharedStorage.getContainer(name)) {
			resp.setStatusCode(HttpStatus.SC_NOT_FOUND);
		}
	}
	//
	private void handleContainerDelete(final HttpResponse resp, final String name) {
		if(!sharedStorage.deleteContainer(name)) {
			resp.setStatusCode(HttpStatus.SC_NOT_FOUND);
		}
	}
}
