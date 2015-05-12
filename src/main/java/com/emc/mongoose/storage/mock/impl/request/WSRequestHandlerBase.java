package com.emc.mongoose.storage.mock.impl.request;
//
import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.net.ServiceUtils;
//
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.impl.data.src.UniformDataSource;
//
import com.emc.mongoose.storage.mock.api.data.WSObjectMock;
import com.emc.mongoose.storage.mock.impl.cinderella.BasicRequestConsumer;
import com.emc.mongoose.storage.mock.impl.cinderella.BasicResponseProducer;
import com.emc.mongoose.storage.mock.impl.cinderella.Cinderella;
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
import javax.management.MBeanServer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by andrey on 13.05.15.
 */
public abstract class WSRequestHandlerBase
implements HttpAsyncRequestHandler<HttpRequest> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static int RING_OFFSET_RADIX = RunTimeConfig.getContext().getDataRadixOffset();
	// metrics section start
	private final static MetricRegistry METRICS = new MetricRegistry();
	private final static MBeanServer MBEAN_SRV = ServiceUtils.getMBeanServer(
		RunTimeConfig.getContext().getRemotePortExport()
	);
	private final static JmxReporter METRICS_REPORTER = JmxReporter.forRegistry(METRICS)
		.convertDurationsTo(TimeUnit.SECONDS)
		.convertRatesTo(TimeUnit.SECONDS)
		.registerWith(MBEAN_SRV)
		.build();
	protected final static String
		METRIC_COUNT = "count",
		ALL_METHODS = "all",
		METHOD_PUT = "put",
		METHOD_GET = "get",
		METHOD_POST = "post",
		METHOD_HEAD = "head",
		METHOD_DELETE = "delete",
		METHOD_TRACE = "trace";
	private final static Counter
		COUNT_SUCC_CREATE = METRICS.counter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.CREATE), METRIC_COUNT,
				LoadExecutor.METRIC_NAME_SUCC
			)
		),
		COUNT_SUCC_READ = METRICS.counter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.READ), METRIC_COUNT,
				LoadExecutor.METRIC_NAME_SUCC
			)
		),
		COUNT_SUCC_DELETE = METRICS.counter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.DELETE), METRIC_COUNT,
				LoadExecutor.METRIC_NAME_SUCC
			)
		),
		COUNTER_FAIL_CREATE = METRICS.counter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.CREATE), METRIC_COUNT,
				LoadExecutor.METRIC_NAME_FAIL
			)
		), COUNTER_FAIL_READ = METRICS.counter(
		MetricRegistry.name(
			Cinderella.class, String.valueOf(IOTask.Type.READ), METRIC_COUNT,
			LoadExecutor.METRIC_NAME_FAIL
		)
	);
	private final static Meter
		BW_ALL = METRICS.meter(
			MetricRegistry.name(Cinderella.class, ALL_METHODS, LoadExecutor.METRIC_NAME_BW)
		),
		BW_CREATE = METRICS.meter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.CREATE), LoadExecutor.METRIC_NAME_BW
			)
		),
		BW_READ = METRICS.meter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.READ), LoadExecutor.METRIC_NAME_BW
			)
		),
		TP_ALL = METRICS.meter(
			MetricRegistry.name(
				Cinderella.class, ALL_METHODS, LoadExecutor.METRIC_NAME_TP
			)
		),
		TP_CREATE = METRICS.meter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.CREATE), LoadExecutor.METRIC_NAME_TP
			)
		),
		TP_READ = METRICS.meter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.READ), LoadExecutor.METRIC_NAME_TP
			)
		);
	static {
		METRICS_REPORTER.start();
	}
	// metrics section end
	private final static AtomicLong NEXT_OFFSET = new AtomicLong(
		Math.abs(System.nanoTime() ^ ServiceUtils.getHostAddrCode())
	);
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
		return BasicRequestConsumer.getInstance();
	}
	//
	@Override
	public final void handle(
		final HttpRequest httpRequest, final HttpAsyncExchange httpExchange,
		final HttpContext httpContext
	) {
		// load rate limitation algorithm
		if(rateLimit > 0) {
			if(TP_ALL.getMeanRate() > rateLimit) {
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
		httpExchange.submitResponse(BasicResponseProducer.getInstance(httpResponse));
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
			COUNT_SUCC_CREATE.inc();
			BW_CREATE.mark(dataObject.getSize());
			BW_ALL.mark(dataObject.getSize());
			TP_CREATE.mark();
			TP_ALL.mark();
		} catch(final HttpException e) {
			httpResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			LogUtil.failure(LOG, Level.ERROR, e, "Put method failure");
			COUNTER_FAIL_CREATE.inc();
		} catch(final NumberFormatException e){
			httpResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			LogUtil.failure(
				LOG, Level.ERROR, e, "Put method failure.Data offset doesn't decode."
			);
			COUNTER_FAIL_CREATE.inc();
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
			COUNTER_FAIL_READ.inc();
		} else {
			response.setStatusCode(HttpStatus.SC_OK);
			if(LOG.isTraceEnabled(LogUtil.MSG)) {
				LOG.trace(LogUtil.MSG, String.format("Send data object with ID: %s", dataId));
			}
			response.setEntity(dataObject);
			COUNT_SUCC_READ.inc();
			BW_ALL.mark(dataObject.getSize());
			BW_READ.mark(dataObject.getSize());
			TP_ALL.mark();
			TP_READ.mark();
		}
	}
	//
	private void handleDelete(final HttpResponse httpResponse){
		if(LOG.isTraceEnabled(LogUtil.MSG)) {
			LOG.trace(LogUtil.MSG, "Delete data object: response OK");
		}
		httpResponse.setStatusCode(HttpStatus.SC_OK);
		COUNT_SUCC_DELETE.inc();
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
