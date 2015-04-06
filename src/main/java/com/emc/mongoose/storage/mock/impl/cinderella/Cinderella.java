package com.emc.mongoose.storage.mock.impl.cinderella;
//
import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
//
import com.emc.mongoose.common.collections.Cache;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.net.ServiceUtils;
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
//
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.impl.data.src.UniformDataSource;
import com.emc.mongoose.storage.adapter.atmos.WSSubTenantImpl;
import com.emc.mongoose.storage.adapter.swift.WSRequestConfigImpl;
import com.emc.mongoose.storage.mock.api.data.WSObjectMock;
import com.emc.mongoose.storage.mock.impl.data.BasicWSObjectMock;
//
import org.apache.commons.codec.binary.Base64;
//
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections4.map.LRUMap;
//
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.DefaultHttpServerIODispatch;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.NHttpConnectionFactory;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.nio.protocol.HttpAsyncService;
import org.apache.http.nio.protocol.UriHttpAsyncRequestHandlerMapper;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.management.MBeanServer;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by olga on 28.01.15.
 */
public final class Cinderella
implements Runnable {
	//
	private final static Map<String, WSObjectMock> SHARED_STORAGE = Collections.synchronizedMap(
		new Cache<String, WSObjectMock>(RunTimeConfig.getContext().getStorageMockCapacity())
	);
	private final static Logger LOG = LogManager.getLogger();
	private final ExecutorService multiSocketSvc;
	private final HttpAsyncService protocolHandler;
	private final static NHttpConnectionFactory<DefaultNHttpServerConnection>
		CONNECTION_FACTORY = new FaultingConnectionFactory(ConnectionConfig.DEFAULT);
	private final JmxReporter metricsReporter;
	private final static long METRICS_UPDATE_PERIOD_SEC = RunTimeConfig.getContext().getLoadMetricsPeriodSec();
	private final static String
		ALL_METHODS = "all",
		METRIC_COUNT = "count",
		METHOD_PUT = "put",
		METHOD_POST = "post",
		METHOD_GET = "get",
		METHOD_HEAD = "head",
		METHOD_DELETE = "delete",
		AUTH = "auth",
		REST = "rest",
		API_NAME = RunTimeConfig.getContext().getApiName(),
		URI_SVC_BASE_PATH = RunTimeConfig.getContext().getString(WSRequestConfigImpl.KEY_CONF_SVC_BASEPATH),
		NAME_SERVER = String.format(
			"%s/%s", Cinderella.class.getSimpleName(), RunTimeConfig.getContext().getRunVersion()
		);
	private static Counter
		counterSuccCreate, counterSuccRead, counterSuccDelete,
		counterFailCreate, counterFailRead, counterFailDelete;
	private static Meter
		allBW, createBW, readBW,
		allTP, createTP, readTP;
	private final static int
		PORT_START = RunTimeConfig.getContext().getApiTypePort(API_NAME),
		// count of heads = 1 head or config count
		COUNT_HEADS = Math.max(1, RunTimeConfig.getContext().getStorageMockHeadCount());
	//
	public Cinderella()
	throws IOException {
		final MetricRegistry metrics = new MetricRegistry();
		final MBeanServer mBeanServer;
		mBeanServer = ServiceUtils.getMBeanServer(RunTimeConfig.getContext().getRemotePortExport());
		metricsReporter = JmxReporter.forRegistry(metrics)
			.convertDurationsTo(TimeUnit.SECONDS)
			.convertRatesTo(TimeUnit.SECONDS)
			.registerWith(mBeanServer)
			.build();
		// init metrics
		counterSuccCreate = metrics.counter(MetricRegistry.name(Cinderella.class,
			String.valueOf(IOTask.Type.CREATE), METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterSuccRead = metrics.counter(MetricRegistry.name(Cinderella.class,
			String.valueOf(IOTask.Type.READ), METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterSuccDelete = metrics.counter(MetricRegistry.name(Cinderella.class,
			String.valueOf(IOTask.Type.DELETE), METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		//
		counterFailCreate = metrics.counter(MetricRegistry.name(Cinderella.class,
			String.valueOf(IOTask.Type.CREATE), METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		counterFailRead = metrics.counter(MetricRegistry.name(Cinderella.class,
			String.valueOf(IOTask.Type.READ), METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		counterFailDelete = metrics.counter(MetricRegistry.name(Cinderella.class,
			String.valueOf(IOTask.Type.DELETE), METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		//
		allTP = metrics.meter(MetricRegistry.name(Cinderella.class,
			ALL_METHODS, LoadExecutor.METRIC_NAME_TP));
		createTP = metrics.meter(MetricRegistry.name(Cinderella.class,
			String.valueOf(IOTask.Type.CREATE), LoadExecutor.METRIC_NAME_TP));
		readTP = metrics.meter(MetricRegistry.name(Cinderella.class,
			String.valueOf(IOTask.Type.READ), LoadExecutor.METRIC_NAME_TP));
		//
		allBW = metrics.meter(MetricRegistry.name(Cinderella.class,
			ALL_METHODS, LoadExecutor.METRIC_NAME_BW));
		createBW = metrics.meter(MetricRegistry.name(Cinderella.class,
			String.valueOf(IOTask.Type.CREATE), LoadExecutor.METRIC_NAME_BW));
		readBW = metrics.meter(MetricRegistry.name(Cinderella.class,
			String.valueOf(IOTask.Type.CREATE), LoadExecutor.METRIC_NAME_BW));
		//
		metricsReporter.start();
		LOG.info(LogUtil.MSG, "Starting with {} heads", COUNT_HEADS);
		// Set up the HTTP protocol processor
		final HttpProcessor httpproc = HttpProcessorBuilder.create()
			.add(new ResponseDate())
			.add(new ResponseServer(NAME_SERVER))
			.add(new ResponseContent())
			.add(new ResponseConnControl()).build();
		// Create request handler registry
		final UriHttpAsyncRequestHandlerMapper reqistry = new UriHttpAsyncRequestHandlerMapper();
		// Register the default handler for all URIs
		reqistry.register("*", new RequestHandler());
		protocolHandler = new HttpAsyncService(httpproc, reqistry);
		multiSocketSvc = Executors.newFixedThreadPool(
			COUNT_HEADS, new NamingWorkerFactory("cinderellaWorker")
		);
	}

	@Override
	public void run() {
		//if there is data src file path
		final String dataFilePath = RunTimeConfig.getContext().getDataSrcFPath();
		final int dataSizeRadix = RunTimeConfig.getContext().getDataRadixSize();
		if (!dataFilePath.isEmpty()){
			try {
				final FileReader reader = new FileReader(dataFilePath);
				final BufferedReader bufferReader = new BufferedReader(reader);
				String s;
				while((s = bufferReader.readLine()) != null) {
					final WSObjectMock dataObject = new BasicWSObjectMock(s) ;
					//if mongoose v.0.5.0
					if (dataSizeRadix == 0x10) {
						dataObject.setSize(Long.valueOf(String.valueOf(dataObject.getSize()), 0x10));
					}
					//
					LOG.trace(LogUtil.DATA_LIST, String.format("%s", dataObject));
					synchronized (SHARED_STORAGE){
						SHARED_STORAGE.put(dataObject.getId(), dataObject);
					}
				}
				reader.close();
			} catch (final FileNotFoundException e) {
				LogUtil.failure(LOG, Level.ERROR, e,
					"File not found.");
			} catch (final IOException e) {
				LogUtil.failure(LOG, Level.ERROR, e,
					"Read line is fault.");
			}
		}
		//

		for(int nextPort = PORT_START; nextPort < PORT_START + COUNT_HEADS; nextPort ++){
			try {
				multiSocketSvc.submit(new WorkerTask(protocolHandler, nextPort));
			} catch(final IOReactorException e) {
				LogUtil.failure(
					LOG, Level.ERROR, e,
					String.format("Failed to start the head at port #%d", nextPort)
				);
			}
		}
		if (COUNT_HEADS > 1) {
			LOG.info(LogUtil.MSG,"Listening the ports {} .. {}", PORT_START, PORT_START + COUNT_HEADS - 1);
		} else {
			LOG.info(LogUtil.MSG,"Listening the port {}", PORT_START);
		}
		multiSocketSvc.shutdown();
		try {
			//output metrics
			final long updatePeriodMilliSec = TimeUnit.SECONDS.toMillis(METRICS_UPDATE_PERIOD_SEC);
			while (METRICS_UPDATE_PERIOD_SEC > 0) {
				printMetrics();
				Thread.sleep(updatePeriodMilliSec);
			}
			//
			final long timeOutValue = RunTimeConfig.getContext().getLoadLimitTimeValue();
			final TimeUnit timeUnit = RunTimeConfig.getContext().getLoadLimitTimeUnit();
			if(timeOutValue > 0) {
				multiSocketSvc.awaitTermination(timeOutValue, timeUnit);
			} else {
				multiSocketSvc.awaitTermination(Long.MAX_VALUE, timeUnit);
			}
		} catch (final InterruptedException e) {
			LOG.info(LogUtil.MSG, "Interrupting the Cinderella");
		} finally {
			metricsReporter.close();
		}
	}
	//
	private final static String
		MSG_FMT_METRICS = "countSucc=(%d/%d/%d); countFail=(%d/%d/%d)" +
		"TP[/s]=(%.3f/%.3f/%.3f/%.3f); BW[MB/s]=(%.3f/%.3f/%.3f/%.3f)";
	//
	private void printMetrics() {
		LOG.info(
			LogUtil.PERF_AVG,
			String.format(
				LogUtil.LOCALE_DEFAULT, MSG_FMT_METRICS,
				//
				counterSuccCreate.getCount(), counterSuccRead.getCount(), counterSuccDelete.getCount(),
				counterFailCreate.getCount(), counterFailRead.getCount(), counterFailDelete.getCount(),
				//
				allTP.getMeanRate(),
				allTP.getOneMinuteRate(),
				allTP.getFiveMinuteRate(),
				allTP.getFifteenMinuteRate(),
				//
				allBW.getMeanRate() / LoadExecutor.MIB,
				allBW.getOneMinuteRate() / LoadExecutor.MIB,
				allBW.getFiveMinuteRate() / LoadExecutor.MIB,
				allBW.getFifteenMinuteRate() / LoadExecutor.MIB
			)
		);
	}
	///////////////////////////////////////////////////////////////////////////////////////////////////
	//Mock Handler
	///////////////////////////////////////////////////////////////////////////////////////////////////
	private static final class RequestHandler
	implements HttpAsyncRequestHandler<HttpRequest> {
		//
		private final static int RING_OFFSET_RADIX = RunTimeConfig.getContext().getDataRadixOffset();
		private static AtomicLong NEXT_OFFSET = new AtomicLong(
			Math.abs(System.nanoTime() ^ ServiceUtils.getHostAddrCode())
		);
		//
		public RequestHandler() {
			super();
		}
		//
		@Override
		public HttpAsyncRequestConsumer<HttpRequest> processRequest(
			final HttpRequest request, final HttpContext context
		) throws HttpException, IOException {
			return new BasicRequestConsumer();
		}
		//
		@Override
		public final void handle(
			final HttpRequest request, final HttpAsyncExchange httpexchange,
			final HttpContext context
		){
			final HttpResponse response = httpexchange.getResponse();
			final String method = request.getRequestLine().getMethod().toLowerCase(Locale.ENGLISH);
			//Get URI components
			final String[] requestUri = request.getRequestLine().getUri().split("/");
			final String dataId = requestUri[requestUri.length - 1];
			//
			if(isAtmosReq(requestUri)){
				if(isAtmosSubtenantReq(requestUri)){
					handleSubtenantReq(response, method);
				} else {
					if (isAtmosObjectReq(requestUri)) {
						LOG.trace(LogUtil.MSG, "Handle atmos object request. URI doesn't contain the object ID.");
						handleAtmosObjectDataReq(response, request, method, dataId);
					} else {
						LOG.trace(LogUtil.MSG, "Handle atmos request. URI contains the object ID.");
						handleGenericDataReq(response, request, method, dataId);
					}
				}
			} else if(isSwiftAuthTokenReq(requestUri)){
				handleSwiftAuthTokenReq(response);
			} else if(isSwiftReq(requestUri)){
				if(isSwiftContanerReq(requestUri, method)){
					LOG.trace(LogUtil.MSG, "Create contaner: response OK.");
					response.setStatusCode(HttpStatus.SC_OK);
				} else {
					LOG.trace(LogUtil.MSG, "Handle swift request.");
					handleGenericDataReq(response, request, method, dataId);
				}
			}else if (isS3BucketReq(requestUri, method)){
				LOG.trace(LogUtil.MSG, "Create s3 bucket: response OK.");
				response.setStatusCode(HttpStatus.SC_OK);
			} else {
				LOG.trace(LogUtil.MSG, "Handle S3 request.");
				handleGenericDataReq(response, request, method, dataId);
			}
			httpexchange.submitResponse(new BasicResponseProducer(response));
		}
		//
		private static boolean isAtmosReq(final String[] requestUri){
			return requestUri[1].equals(REST);
		}
		//
		private static boolean isAtmosSubtenantReq(final String[] requestUri){
			return requestUri[2].equals(WSSubTenantImpl.SUBTENANT);
		}
		//
		private static boolean isAtmosObjectReq(final String[] requestUri){
			return requestUri[2].equals(com.emc.mongoose.storage.adapter.atmos.WSRequestConfigImpl.API_TYPE_OBJ);
		}
		//
		private static boolean isS3BucketReq(final String[] requestUri, final String method){
			return method.equals(METHOD_PUT) && requestUri.length == 2;
		}
		//
		private static boolean isSwiftReq(final String[] requestUri){
			return requestUri[1].equals(URI_SVC_BASE_PATH);
		}
		//
		private static boolean isSwiftAuthTokenReq(final String[] requestUri){
			return requestUri[1].equals(AUTH);
		}
		//
		private static boolean isSwiftContanerReq(final String[] requestUri, final String method){
			return requestUri[1].equals(URI_SVC_BASE_PATH) &&
				requestUri.length == 4 && method.equals(METHOD_PUT);
		}
		//
		private void handleSwiftAuthTokenReq(final HttpResponse response){
			LOG.trace(LogUtil.MSG, "Create auth token ");
			response.setStatusCode(HttpStatus.SC_OK);
			response.setHeader(WSRequestConfigImpl.KEY_X_AUTH_TOKEN, randomString(5));
		}
		//
		private void handleSubtenantReq(final HttpResponse response, final String method){
			LOG.trace(LogUtil.MSG, "Create atmos subtenant");
			if(method.equals(METHOD_PUT)) {
				response.setHeader(WSSubTenantImpl.KEY_SUBTENANT_ID, randomString(5));
			}
			response.setStatusCode(HttpStatus.SC_OK);
		}
		//
		private void handleAtmosObjectDataReq(
			final HttpResponse response, final HttpRequest request,
			final String method, String dataId
		) {
			if (method.equals(METHOD_POST)) {
				dataId = generateId();
				final String headerLocation = String.format(
					com.emc.mongoose.storage.adapter.atmos.WSRequestConfigImpl.FMT_SLASH,
					request.getRequestLine().getUri(), dataId);
				response.setHeader(HttpHeaders.LOCATION, headerLocation);
			}
			handleGenericDataReq(response, request, method, dataId);
		}
		//
		private void handleDeleteReq(final HttpResponse response){
			LOG.trace(LogUtil.MSG, "Delete data object: response OK");
			response.setStatusCode(HttpStatus.SC_OK);
			counterSuccDelete.inc();
		}
		//
		private void handleGenericDataReq(
			final HttpResponse response, final HttpRequest request,
			final String method, final String dataId){
			switch (method){
				case METHOD_POST:
					handleCreate(response, request, dataId);
					break;
				case METHOD_PUT:
					handleCreate(response, request, dataId);
					break;
				case METHOD_GET:
					handleRead(response, dataId);
					break;
				case METHOD_HEAD:
					response.setStatusCode(HttpStatus.SC_OK);
					break;
				case METHOD_DELETE:
					handleDeleteReq(response);
					break;
			}

		}
		//
		private static void handleCreate(final HttpResponse response, final HttpRequest request, final String dataId){
			LOG.trace(LogUtil.MSG, String.format("Create data object with ID: %s", dataId));
			try {
				response.setStatusCode(HttpStatus.SC_OK);
				final WSObjectMock dataObject = writeDataObject(request, dataId);
				counterSuccCreate.inc();
				createBW.mark(dataObject.getSize());
				allBW.mark(dataObject.getSize());
				createTP.mark();
				allTP.mark();
			}catch (final HttpException e){
				response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				LogUtil.failure(LOG, Level.ERROR, e, "Put method failure");
				counterFailCreate.inc();
			}catch (final NumberFormatException e){
				response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				LogUtil.failure(LOG, Level.ERROR, e, "Put method failure.Data offset doesn't decode.");
				counterFailCreate.inc();
			}
		}
		//
		private static void handleRead(final HttpResponse response, final String dataId){
			if(SHARED_STORAGE.containsKey(dataId)) {
				response.setStatusCode(HttpStatus.SC_OK);
				LOG.trace(LogUtil.MSG, String.format("Send data object with ID: %s", dataId));
				final WSObjectMock dataObject = SHARED_STORAGE.get(dataId);
				response.setEntity(dataObject);
				counterSuccRead.inc();
				allBW.mark(dataObject.getSize());
				readBW.mark(dataObject.getSize());
				allTP.mark();
				readTP.mark();
			} else {
				response.setStatusCode(HttpStatus.SC_NOT_FOUND);
				LOG.trace(LogUtil.ERR, String.format("No such object: %s", dataId));
				counterFailRead.inc();
			}
		}
		//
		private static String generateId(){
			long offset = NEXT_OFFSET.getAndSet(Math.abs(UniformDataSource.nextWord(NEXT_OFFSET.get())));
			return Long.toString(offset, DataObject.ID_RADIX);
		}
		//
		private static String randomString(final int len)
		{
			final byte buff[] = new byte[len];
			ThreadLocalRandom.current().nextBytes(buff);
			return Hex.encodeHexString(buff);
		}
		//
		private static WSObjectMock writeDataObject(
			final HttpRequest request, final String dataID
		) throws HttpException, NumberFormatException{
			WSObjectMock dataObject;
			final HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
			final long bytes = entity.getContentLength();
			//create data object or get it for append or update
			if (SHARED_STORAGE.containsKey(dataID)) {
				dataObject = SHARED_STORAGE.get(dataID);
			}else {
				final long offset = genOffset(dataID);
				dataObject = new BasicWSObjectMock(dataID, offset, bytes);
			}
			LOG.trace(LogUtil.DATA_LIST, String.format("%s", dataObject));
			synchronized (SHARED_STORAGE) {
				SHARED_STORAGE.put(dataID, dataObject);
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
			if (RING_OFFSET_RADIX == 0x40) { // base64
				offset = ByteBuffer
					.allocate(Long.SIZE / Byte.SIZE)
					.put(Base64.decodeBase64(dataID))
					.getLong(0);
			} else if (RING_OFFSET_RADIX > 1 && RING_OFFSET_RADIX <= Character.MAX_RADIX) {
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
	}
	///////////////////////////////////////////////////////////////////////////////////////////////////
	// WorkerTask
	///////////////////////////////////////////////////////////////////////////////////////////////////
	private final static class WorkerTask
	implements Runnable {
		//
		private final ListeningIOReactor ioReactor;
		private final IOEventDispatch ioEventDispatch;
		private final int port;
		//
		public WorkerTask(
			final HttpAsyncService protocolHandler,
			final int port
		) throws IOReactorException {
			this.port = port;
			ioEventDispatch = new DefaultHttpServerIODispatch(protocolHandler, CONNECTION_FACTORY);
			// Set I/O reactor defaults
			final RunTimeConfig localRunTimeConfig = RunTimeConfig.getContext();
			final IOReactorConfig config = IOReactorConfig.custom()
				.setIoThreadCount(localRunTimeConfig.getStorageMockIoThreadsPerSocket())
					.setSoTimeout(localRunTimeConfig.getSocketTimeOut())
				.setConnectTimeout(localRunTimeConfig.getConnTimeOut())
				.build();
			// Create server-side I/O reactor
			ioReactor = new DefaultListeningIOReactor(config);
		}
		//
		@Override
		public final void run() {
			try {
				// Listen of the given port
				ioReactor.listen(new InetSocketAddress(port));
				// Ready to go!
				ioReactor.execute(ioEventDispatch);
			} catch (final InterruptedIOException ex) {
				LogUtil.failure(LOG, Level.DEBUG, ex, "Interrupted");
			} catch (final IOReactorException ex) {
				LogUtil.failure(LOG, Level.ERROR, ex, "I/O reactor failure");
			} catch (final IOException ex) {
				LogUtil.failure(LOG, Level.ERROR, ex, "I/O failure");
			}
		}
	}
}
