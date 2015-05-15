package com.emc.mongoose.storage.mock.impl.cinderella.request;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.net.ServiceUtils;
//
import com.emc.mongoose.core.api.data.DataObject;
//
import com.emc.mongoose.core.impl.data.src.UniformDataSource;
//
import com.emc.mongoose.storage.mock.api.data.WSObjectMock;
import com.emc.mongoose.storage.mock.impl.cinderella.response.BasicWSResponseProducer;
import com.emc.mongoose.storage.mock.impl.cinderella.WSRequestMetrics;
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
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by andrey on 13.05.15.
 */
public abstract class WSRequestHandlerBase
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
	private final static AtomicLong NEXT_OFFSET = new AtomicLong(
		Math.abs(System.nanoTime() ^ ServiceUtils.getHostAddrCode())
	);
	public final static WSRequestMetrics METRICS = new WSRequestMetrics(RunTimeConfig.getContext());
	//
	private final float rateLimit;
	private final AtomicInteger lastMilliDelay = new AtomicInteger(1);
	private final Map<String, WSObjectMock> sharedStorage;
	//
	protected WSRequestHandlerBase(
		final RunTimeConfig runTimeConfig, final Map<String, WSObjectMock> sharedStorage
	) {
		this.rateLimit = runTimeConfig.getLoadLimitRate();
		this.sharedStorage = sharedStorage;
	}
	//
	@Override
	public final HttpAsyncRequestConsumer<HttpRequest> processRequest(
		final HttpRequest request, final HttpContext context
	) throws HttpException, IOException {
		return BasicWSRequestConsumer.getInstance();
	}
	//
	@Override
	public final void handle(
		final HttpRequest httpRequest, final HttpAsyncExchange httpExchange,
		final HttpContext httpContext
	) {
		// load rate limitation algorithm
		if(rateLimit > 0) {
			if(METRICS.getMeanRate() > rateLimit) {
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
		final HttpRequest httpRequest, final HttpResponse httpResponse, final String method,
		final String requestURI[], final String dataId
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
				handleDelete(httpResponse);
				break;
		}
	}
	//
	private void handleCreate(
		final HttpRequest httpRequest, final HttpResponse httpResponse, final String dataId
	) {
		if(LOG.isTraceEnabled(LogUtil.MSG)) {
			LOG.trace(LogUtil.MSG, String.format("Create data object with ID: %s", dataId));
		}
		try {
			httpResponse.setStatusCode(HttpStatus.SC_OK);
			final WSObjectMock dataObject = writeDataObject(httpRequest, dataId);
			METRICS.markCreate(dataObject.getSize());
		} catch(final HttpException e) {
			httpResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			LogUtil.failure(LOG, Level.ERROR, e, "Put method failure");
			METRICS.markCreate(-1);
		} catch(final NumberFormatException e){
			httpResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			LogUtil.failure(
				LOG, Level.ERROR, e, "Put method failure.Data offset doesn't decode."
			);
			METRICS.markCreate(-1);
		}
	}
	//
	private void handleRead(final HttpResponse response, final String dataId) {
		final WSObjectMock dataObject = sharedStorage.get(dataId);
		if(dataObject == null) {
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
			if(LOG.isTraceEnabled(LogUtil.MSG)) {
				LOG.trace(LogUtil.ERR, String.format("No such object: %s", dataId));
			}
			METRICS.markRead(-1);
		} else {
			response.setStatusCode(HttpStatus.SC_OK);
			if(LOG.isTraceEnabled(LogUtil.MSG)) {
				LOG.trace(LogUtil.MSG, String.format("Send data object with ID: %s", dataId));
			}
			response.setEntity(dataObject);
			METRICS.markRead(dataObject.getSize());
		}
	}
	//
	private void handleDelete(final HttpResponse httpResponse){
		if(LOG.isTraceEnabled(LogUtil.MSG)) {
			LOG.trace(LogUtil.MSG, "Delete data object: response OK");
		}
		httpResponse.setStatusCode(HttpStatus.SC_OK);
		METRICS.markDelete();
	}
	//
	private WSObjectMock writeDataObject(final HttpRequest request, final String dataID)
		throws HttpException, NumberFormatException {
		final HttpEntity entity = HttpEntityEnclosingRequest.class.cast(request).getEntity();
		final long bytes = entity.getContentLength();
		//create data object or get it for append or update
		final long offset = genOffset(dataID);
		final WSObjectMock dataObject = new BasicWSObjectMock(dataID, offset, bytes);
		sharedStorage.put(dataID, dataObject);
		if(LOG.isTraceEnabled(LogUtil.DATA_LIST)) {
			LOG.trace(LogUtil.DATA_LIST, String.format("%s", dataObject));
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
	private static long genOffset(final String dataID)
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
			throw new HttpException(
				String.format(
					"Unsupported data ring offset radix: %d", RING_OFFSET_RADIX
				)
			);
		}
		return offset;
	}
	//
	protected static String generateId(){
		long offset = NEXT_OFFSET.getAndSet(
			Math.abs(UniformDataSource.nextWord(NEXT_OFFSET.get()))
		);
		return Long.toString(offset, DataObject.ID_RADIX);
	}
}
