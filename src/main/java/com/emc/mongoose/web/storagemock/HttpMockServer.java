package com.emc.mongoose.web.storagemock;
//
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.logging.TraceLogger;
import com.emc.mongoose.util.remote.ServiceUtils;
import com.emc.mongoose.util.threading.WorkerFactory;
//
import com.emc.mongoose.web.api.WSIOTask;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
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
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
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
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
//
/**
 * Created by olga on 28.01.15.
 */
public class HttpMockServer
implements Runnable{
	//
	private final static Logger LOG = LogManager.getLogger();
	private final Map<String, WSObjectMock> mapDataObject;
	private final Queue<String> queueDataId;
	private final ExecutorService multiSocketSvc;
	private final HttpAsyncService protocolHandler;
	private final NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory;
	//
	private final static String NAME_SERVER = "StorageMock/0.1";
	private long metricsUpdatePeriodSec;
	private final static int BASE_16 = 16,
		BASE_36 = 36,
		BASE_64 = 64;
	private final int dataIdRadix;
	private final int portCount;
	private final int portStart;
	private static int MAX_PAGE_SIZE;
	//
	private final RunTimeConfig runTimeConfig;
	//
	private final JmxReporter metricsReporter;
	//
	private final static String
		ALL_METHODS = "all",
		METRIC_COUNT = "count";
	private final Counter
		counterAllSucc, counterAllFail,
		counterGetSucc, counterGetFail,
		counterPostSucc, counterPostFail,
		counterPutSucc, counterPutFail,
		counterDeleteSucc, counterDeleteFail,
		counterHeadSucc;
	private final Histogram durAll, durGet, durPost, durPut, durDelete;
	private final Meter
		allBW, getBW, postBW, putBW, deleteBW,
		allTP, getTP, postTP, putTP, deleteTP;
	//
	public HttpMockServer(final RunTimeConfig runTimeConfig)
	throws IOException {
		this.runTimeConfig = runTimeConfig;
		MAX_PAGE_SIZE = (int) runTimeConfig.getDataPageSize();
		metricsUpdatePeriodSec = runTimeConfig.getRunMetricsPeriodSec();
		//
		final MetricRegistry metrics = new MetricRegistry();
		final MBeanServer mBeanServer;
		mBeanServer = ServiceUtils.getMBeanServer(runTimeConfig.getRemoteExportPort());
		metricsReporter = JmxReporter.forRegistry(metrics)
			.convertDurationsTo(TimeUnit.SECONDS)
			.convertRatesTo(TimeUnit.SECONDS)
			.registerWith(mBeanServer)
			.build();
		//
		// init metrics
		counterAllSucc = metrics.counter(MetricRegistry.name(HttpMockServer.class,
			ALL_METHODS, METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterAllFail = metrics.counter(MetricRegistry.name(HttpMockServer.class,
			ALL_METHODS, METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		durAll = metrics.histogram(MetricRegistry.name(HttpMockServer.class,
			ALL_METHODS, LoadExecutor.METRIC_NAME_DUR));
		allTP = metrics.meter(MetricRegistry.name(HttpMockServer.class,
			ALL_METHODS, LoadExecutor.METRIC_NAME_TP));
		allBW = metrics.meter(MetricRegistry.name(HttpMockServer.class,
			ALL_METHODS, LoadExecutor.METRIC_NAME_BW));
		//
		counterGetSucc = metrics.counter(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.GET.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterGetFail = metrics.counter(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.GET.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		durGet = metrics.histogram(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.GET.name(), LoadExecutor.METRIC_NAME_DUR));
		getBW = metrics.meter(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.GET.name(), LoadExecutor.METRIC_NAME_BW));
		getTP = metrics.meter(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.GET.name(), LoadExecutor.METRIC_NAME_TP));
		//
		counterPostSucc = metrics.counter(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.POST.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterPostFail = metrics.counter(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.POST.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		durPost = metrics.histogram(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.POST.name(), LoadExecutor.METRIC_NAME_DUR));
		postBW = metrics.meter(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.POST.name(), LoadExecutor.METRIC_NAME_BW));
		postTP = metrics.meter(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.POST.name(), LoadExecutor.METRIC_NAME_TP));
		//
		counterPutSucc = metrics.counter(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.PUT.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterPutFail = metrics.counter(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.PUT.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		durPut = metrics.histogram(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.PUT.name(), LoadExecutor.METRIC_NAME_DUR));
		putBW = metrics.meter(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.PUT.name(), LoadExecutor.METRIC_NAME_BW));
		putTP = metrics.meter(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.PUT.name(), LoadExecutor.METRIC_NAME_TP));
		//
		counterDeleteSucc = metrics.counter(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.DELETE.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterDeleteFail = metrics.counter(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.DELETE.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		durDelete = metrics.histogram(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.DELETE.name(), LoadExecutor.METRIC_NAME_DUR));
		deleteBW = metrics.meter(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.DELETE.name(), LoadExecutor.METRIC_NAME_BW));
		deleteTP = metrics.meter(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.DELETE.name(), LoadExecutor.METRIC_NAME_TP));
		//
		counterHeadSucc = metrics.counter(MetricRegistry.name(HttpMockServer.class,
			WSIOTask.HTTPMethod.HEAD.name(), LoadExecutor.METRIC_NAME_SUCC));
		//
		metricsReporter.start();
		//queue size for data object
		final int queueDataIdSize = runTimeConfig.getInt("wsmock.queue.dataobject.size");
		queueDataId = new ArrayBlockingQueue<>(queueDataIdSize);
		mapDataObject = new ConcurrentHashMap<>(queueDataIdSize);
		//
		dataIdRadix = runTimeConfig.getInt("wsmock.radix");
		// count of ports = Count of kernel-1 or 1.
		portCount = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
		LOG.trace(Markers.MSG, " There are {} processors.", portCount);
		final String apiName = runTimeConfig.getStorageApi();
		portStart = runTimeConfig.getInt("api." + apiName + ".port");
		// Set up the HTTP protocol processor
		final HttpProcessor httpproc = HttpProcessorBuilder.create()
			.add(new ResponseDate())
			.add(new ResponseServer(NAME_SERVER))
			.add(new ResponseContent())
			.add(new ResponseConnControl()).build();
		// Create request handler registry
		final UriHttpAsyncRequestHandlerMapper reqistry = new UriHttpAsyncRequestHandlerMapper();
		// Register the default handler for all URIs
		reqistry.register("*", new HttpMockHandler());
		protocolHandler = new HttpAsyncService(httpproc, reqistry);
		//connFactory = new DefaultNHttpServerConnectionFactory(ConnectionConfig.DEFAULT);
		connFactory = new WSMockConnectionFactory(
			ConnectionConfig.DEFAULT, runTimeConfig, counterAllFail);
		multiSocketSvc = Executors.newFixedThreadPool(portCount, new WorkerFactory("workerWSMock"));
	}

	@Override
	public void run() {
		final String dataFilePath = runTimeConfig.getDataSrcFPath();
		if (!dataFilePath.isEmpty()){
			try {
				final FileReader reader = new FileReader(dataFilePath);
				final BufferedReader bufferReader = new BufferedReader(reader);
				String s;
				while((s = bufferReader.readLine()) != null) {
					WSObjectMock dataObject = new BasicWSObjectMock(s) ;
					mapDataObject.put(dataObject.getId(),dataObject);
				}
				reader.close();
			} catch (FileNotFoundException e) {
				TraceLogger.failure(LOG, Level.WARN, e,
					"File not found.");
			} catch (IOException e) {
				TraceLogger.failure(LOG, Level.WARN, e,
					"Read line is fault.");
			}
		}
		for (int i = 0; i < portCount; i++){
			multiSocketSvc.submit(new HeadWrokerTask(protocolHandler, connFactory, portStart + i));
			LOG.info(Markers.MSG, " WSMock can listen {} port.", portStart + i);
		}
		multiSocketSvc.shutdown();
		try {
			//
			final long updatePeriodMilliSec = TimeUnit.SECONDS.toMillis(metricsUpdatePeriodSec);
			while (metricsUpdatePeriodSec > 0) {
				printMetrics();
				Thread.sleep(updatePeriodMilliSec);
			}
			//
			multiSocketSvc.awaitTermination(runTimeConfig.getRunTimeValue(), runTimeConfig.getRunTimeUnit());
		} catch (final InterruptedException e) {
			// do nothing
			LOG.info(Markers.MSG, "Interrupting the WSMock");
		} finally {
			metricsReporter.close();
		}
	}
	//
	private final static String
		MSG_FMT_METRICS = "count=(%d/%d); duration[us]=(%d/%d/%d/%d); " +
		"TP[/s]=(%.3f/%.3f/%.3f/%.3f); BW[MB/s]=(%.3f/%.3f/%.3f/%.3f)";
	//
	private void printMetrics() {
		final Snapshot allDurSnapshot = durAll.getSnapshot();
		LOG.info(
			Markers.PERF_AVG,
			String.format(
				Main.LOCALE_DEFAULT, MSG_FMT_METRICS,
				//
				counterAllSucc.getCount(), counterAllFail.getCount(),
				//
				(int) (allDurSnapshot.getMean() / LoadExecutor.NANOSEC_SCALEDOWN),
				(int) (allDurSnapshot.getMin() / LoadExecutor.NANOSEC_SCALEDOWN),
				(int) (allDurSnapshot.getMedian() / LoadExecutor.NANOSEC_SCALEDOWN),
				(int) (allDurSnapshot.getMax() / LoadExecutor.NANOSEC_SCALEDOWN),
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
	class HttpMockHandler
	implements HttpAsyncRequestHandler<HttpRequest>{
		//
		public HttpMockHandler() {
			super();
		}
		//
		@Override
		public HttpAsyncRequestConsumer<HttpRequest> processRequest(final HttpRequest request, final HttpContext context)
		throws HttpException, IOException
		{
			return new WSMockBasicAcyncRequestConsumer(runTimeConfig);
		}
		//
		@Override
		public void handle(
			final HttpRequest request,
			final HttpAsyncExchange httpexchange,
			final HttpContext context)
		throws HttpException, IOException
		{
			final HttpResponse response = httpexchange.getResponse();
			//HttpCoreContext coreContext = HttpCoreContext.adapt(context);
			String method = request.getRequestLine().getMethod().toLowerCase(Locale.ENGLISH);
			String dataID = "";
			//Get data Id
			try {
				final String[] requestUri = request.getRequestLine().getUri().split("/");
				if (requestUri.length >= 3) {
					dataID = requestUri[2];
				} else {
					method = "head";
				}
			} catch (final NumberFormatException e){
				response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				TraceLogger.failure(
					LOG, Level.WARN, e,
					String.format("Unexpected object id format: \"%s\"", dataID)
				);
			} catch (final ArrayIndexOutOfBoundsException e) {
				response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				TraceLogger.failure(LOG, Level.WARN, e,
					"Request URI is not correct. Data object ID doesn't exist in request URI");
			}
			//
			switch (method){
				case ("put"):
					doPut(request, response, dataID);
					break;
				case ("head"):
					doHead(response);
					break;
				case ("get"):
					doGet(response, dataID);
					break;
			}
			httpexchange.submitResponse(new BasicAsyncResponseProducer(response));
		}
	}
	//
	private void doGet(final HttpResponse response, final String dataID)
	throws  HttpException, IOException
	{
		LOG.trace(Markers.MSG, " Request  method Get ");
		response.setStatusCode(HttpStatus.SC_OK);
		if (mapDataObject.containsKey(dataID)) {
			LOG.trace(Markers.MSG, "   Send data object ", dataID);
			final WSObjectMock object = mapDataObject.get(dataID);
			response.setEntity(object);
			LOG.trace(Markers.MSG, "   Response: OK");
			counterAllSucc.inc();
			getBW.mark(object.getSize());
			allBW.mark(object.getSize());
			getTP.mark();
			allTP.mark();
		} else {
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
			LOG.trace(Markers.ERR, String.format("No such object: \"%s\"", dataID));
			counterAllFail.inc();
			counterGetFail.inc();
		}
	}
	//
	/*
	offset for mongoose versions since v0.6:
		final long offset = Long.valueOf(dataID, WSRequestConfigBase.RADIX);
	offset for mongoose v0.4x and 0.5x:
		final byte dataIdBytes[] = Base64.decodeBase64(dataID);
		final long offset  = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).put(dataIdBytes).getLong(0);
	offset for mongoose versions prior to v.0.4:
		final long offset = Long.valueOf(dataID, 0x10);
	 */
	private void doPut(final HttpRequest request, final HttpResponse response, final String dataID)
	throws  HttpException, IOException
	{
		LOG.trace(Markers.MSG, " Request  method Put ");
		response.setStatusCode(HttpStatus.SC_OK);
		WSObjectMock dataObject = null;
		try {
			final HttpEntity entity =  ((HttpEntityEnclosingRequest)request).getEntity();
			final long bytes = entity.getContentLength();
			//create data object or get it for append or update
			if(mapDataObject.containsKey(dataID)) {
				dataObject = mapDataObject.get(dataID);
			} else {
				long offset;
				if (dataIdRadix == BASE_64) {
					final byte dataIdBytes[] = Base64.decodeBase64(dataID);
					offset  = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).put(dataIdBytes).getLong(0);
				}else if (dataIdRadix > BASE_36) {
					throw new NumberFormatException("Unexpected radix");
				} else {
					offset = Long.valueOf(dataID, dataIdRadix);
				}
				dataObject = new BasicWSObjectMock(dataID, offset, bytes);
			}
			//
			if (request.getHeaders(HttpHeaders.RANGE) != null) {
				final Header[] headers = request.getHeaders(HttpHeaders.RANGE);
				for (final Header header: headers){
					System.out.println(header.getValue());
				}
				//Parse string of ranges information
				/*
				final String[] rangeStringArray = request.getHeaders(HttpHeaders.RANGE).split("\\s*[=,-]\\s*");
				final List<Long> ranges = new ArrayList<>();
				for (int i = 1; i < rangeStringArray.length; i++){
					ranges.add(Long.valueOf(rangeStringArray[i]));
				}
				if (ranges.size() % 2 != 0){
					ranges.add(ranges.get(ranges.size()-1) + bytes);
				}
				// Switch append or update or exception
				//
				//if append
				if (ranges.get(0) == dataObject.getSize()) {
					//append data object
					dataObject.append(bytes);
					//resize data object
					dataObject.setSize(dataObject.getSize() + bytes);
					//end append
					//if update
				} else if (ranges.get(ranges.size() - 1) <= dataObject.getSize()){
					//update data object
					dataObject.updateRanges(ranges);
				} else {
					throw new Exception();
				}
				*/
			}
			//
			synchronized (queueDataId) {
				if (!queueDataId.offer(dataID)) {
					LOG.trace(Markers.MSG, " Queue is full");
					mapDataObject.remove(queueDataId.remove());
					queueDataId.add(dataID);
				}
				mapDataObject.put(dataID, dataObject);
			}
			counterAllSucc.inc();
			counterPutSucc.inc();
			putBW.mark(bytes);
			allBW.mark(bytes);
			putTP.mark();
			allTP.mark();
		}catch (final IllegalStateException  e){
			//???
			response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			TraceLogger.failure(LOG, Level.WARN, e, "Memory is full");
			counterAllFail.inc();
			counterPutFail.inc();
		}catch (final NumberFormatException e) {
			response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			counterAllFail.inc();
			counterPutFail.inc();
			TraceLogger.failure(
				LOG, Level.WARN, e,
				String.format("Unexpected object id format: \"%s\"", dataID)
			);
		}
	}
	//
	private void doHead(final HttpResponse response)
	throws  HttpException, IOException
	{
		LOG.trace(Markers.MSG, " Request  method Head ");
		response.setStatusCode(HttpStatus.SC_OK);
		counterAllSucc.inc();
	}
	///////////////////////////////////////////////////////////////////////////////////////////////////
	//WorkerTask
	///////////////////////////////////////////////////////////////////////////////////////////////////
	class HeadWrokerTask
	implements Runnable{
		//
		private HttpAsyncService protocolHandler;
		private NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory;
		private int port;
		//
		public HeadWrokerTask(final HttpAsyncService protocolHandler,
							  final NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory,
							  final int port)
		{
			this.protocolHandler = protocolHandler;
			this.connFactory = connFactory;
			this.port = port;
		}
		//
		@Override
		public void run() {
			final IOEventDispatch ioEventDispatch = new DefaultHttpServerIODispatch(protocolHandler, connFactory);
			// Set I/O reactor defaults
			final IOReactorConfig config = IOReactorConfig.custom()
				.setIoThreadCount(runTimeConfig.getInt("wsmock.iothreads.persocket"))
				.setSoTimeout(runTimeConfig.getSocketTimeOut())
				.setConnectTimeout(runTimeConfig.getConnTimeOut())
				.build();
			// Create server-side I/O reactor
			ListeningIOReactor ioReactor = null;
			try {
				ioReactor = new DefaultListeningIOReactor(config);
				// Listen of the given port
				ioReactor.listen(new InetSocketAddress(port));
				// Ready to go!
				ioReactor.execute(ioEventDispatch);
			} catch (final InterruptedIOException ex) {
				TraceLogger.failure(LOG, Level.ERROR, ex, "Interrupted.");
			} catch (final IOReactorException ex) {
				TraceLogger.failure(LOG, Level.ERROR, ex, "IO Reactor failed.");
			} catch (final IOException ex) {
				TraceLogger.failure(LOG, Level.ERROR, ex, "I/O error");
			}
		}
	}
}
