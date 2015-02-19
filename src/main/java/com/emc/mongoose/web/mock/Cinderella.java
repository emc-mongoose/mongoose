package com.emc.mongoose.web.mock;
//
import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
//
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
//
import org.apache.commons.collections4.map.LRUMap;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
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
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
/**
 * Created by olga on 28.01.15.
 */
public final class Cinderella
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final ExecutorService multiSocketSvc;
	private final HttpAsyncService protocolHandler;
	private final NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory;
	//
	public final static String NAME_SERVER = String.format(
		"%s/%s", Cinderella.class.getSimpleName(), Main.RUN_TIME_CONFIG.get().getRunVersion()
	);
	private final int portCount;
	private final int portStart;
	//
	private final RunTimeConfig runTimeConfig;
	//
	private final JmxReporter metricsReporter;
	//
	private long metricsUpdatePeriodSec;
	//
	private final static String
		ALL_METHODS = "all",
		METRIC_COUNT = "count";
	private final Counter
		counterAllSucc, counterAllFail,
		counterGetSucc, counterGetFail,
		counterPutSucc, counterPutFail,
		counterDeleteSucc, counterHeadSucc;
	private final Meter
		allBW, getBW, putBW,
		allTP, getTP, putTP;
	//
	public Cinderella(final RunTimeConfig runTimeConfig)
	throws IOException {
		this.runTimeConfig = runTimeConfig;
		metricsUpdatePeriodSec = runTimeConfig.getRunMetricsPeriodSec();
		connFactory = new FaultingConnectionFactory(ConnectionConfig.DEFAULT, runTimeConfig);
		//
		final MetricRegistry metrics = new MetricRegistry();
		final MBeanServer mBeanServer;
		mBeanServer = ServiceUtils.getMBeanServer(runTimeConfig.getRemoteExportPort());
		metricsReporter = JmxReporter.forRegistry(metrics)
			.convertDurationsTo(TimeUnit.SECONDS)
			.convertRatesTo(TimeUnit.SECONDS)
			.registerWith(mBeanServer)
			.build();
		// init metrics
		counterAllSucc = metrics.counter(MetricRegistry.name(Cinderella.class,
			ALL_METHODS, METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterAllFail = metrics.counter(MetricRegistry.name(Cinderella.class,
			ALL_METHODS, METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		allTP = metrics.meter(MetricRegistry.name(Cinderella.class,
			ALL_METHODS, LoadExecutor.METRIC_NAME_TP));
		allBW = metrics.meter(MetricRegistry.name(Cinderella.class,
			ALL_METHODS, LoadExecutor.METRIC_NAME_BW));
		//
		counterGetSucc = metrics.counter(MetricRegistry.name(Cinderella.class,
			WSIOTask.HTTPMethod.GET.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterGetFail = metrics.counter(MetricRegistry.name(Cinderella.class,
			WSIOTask.HTTPMethod.GET.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		getBW = metrics.meter(MetricRegistry.name(Cinderella.class,
			WSIOTask.HTTPMethod.GET.name(), LoadExecutor.METRIC_NAME_BW));
		getTP = metrics.meter(MetricRegistry.name(Cinderella.class,
			WSIOTask.HTTPMethod.GET.name(), LoadExecutor.METRIC_NAME_TP));
		//
		counterPutSucc = metrics.counter(MetricRegistry.name(Cinderella.class,
			WSIOTask.HTTPMethod.PUT.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterPutFail = metrics.counter(MetricRegistry.name(Cinderella.class,
			WSIOTask.HTTPMethod.PUT.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		putBW = metrics.meter(MetricRegistry.name(Cinderella.class,
			WSIOTask.HTTPMethod.PUT.name(), LoadExecutor.METRIC_NAME_BW));
		putTP = metrics.meter(MetricRegistry.name(Cinderella.class,
			WSIOTask.HTTPMethod.PUT.name(), LoadExecutor.METRIC_NAME_TP));
		//
		counterDeleteSucc = metrics.counter(MetricRegistry.name(Cinderella.class,
			WSIOTask.HTTPMethod.DELETE.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		//
		counterHeadSucc = metrics.counter(MetricRegistry.name(Cinderella.class,
			WSIOTask.HTTPMethod.HEAD.name(), LoadExecutor.METRIC_NAME_SUCC));
		//
		metricsReporter.start();
		//queue size for data object
		final int queueDataIdSize = runTimeConfig.getInt("storage.mock.capacity");
		final Map<String, WSObjectMock> sharedStorage = Collections.synchronizedMap(new LRUMap<String, WSObjectMock>(queueDataIdSize));
		// count of heads = count of cores - 1
		portCount = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
		LOG.debug(Markers.MSG, "Starting w/ {} heads", portCount);
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
		reqistry.register("*", new RequestHandler(sharedStorage));
		protocolHandler = new HttpAsyncService(httpproc, reqistry);
		multiSocketSvc = Executors.newFixedThreadPool(
			portCount, new WorkerFactory("cinderellaWorker")
		);
	}

	@Override
	public void run() {
		//
		for(int nextPort = portStart; nextPort < portStart + portCount; nextPort ++){
			try {
				multiSocketSvc.submit(new WorkerTask(protocolHandler, connFactory, nextPort));
				LOG.info(Markers.MSG, "Listening the port #{}", nextPort);
			} catch(final IOReactorException e) {
				TraceLogger.failure(
					LOG, Level.ERROR, e,
					String.format("Failed to start the head at port #%d", nextPort)
				);
			}
		}
		multiSocketSvc.shutdown();
		try {
			//output metrics
			final long updatePeriodMilliSec = TimeUnit.SECONDS.toMillis(metricsUpdatePeriodSec);
			while (metricsUpdatePeriodSec > 0) {
				printMetrics();
				Thread.sleep(updatePeriodMilliSec);
			}
			//
			multiSocketSvc.awaitTermination(
				runTimeConfig.getRunTimeValue(), runTimeConfig.getRunTimeUnit()
			);
		} catch (final InterruptedException e) {
			LOG.info(Markers.MSG, "Interrupting the Cinderella");
		} finally {
			metricsReporter.close();
		}
	}
	//
	private final static String
		MSG_FMT_METRICS = "count=(%d/%d); " +
		"TP[/s]=(%.3f/%.3f/%.3f/%.3f); BW[MB/s]=(%.3f/%.3f/%.3f/%.3f)";
	//
	private void printMetrics() {
		LOG.info(
			Markers.PERF_AVG,
			String.format(
				Main.LOCALE_DEFAULT, MSG_FMT_METRICS,
				//
				counterAllSucc.getCount(), counterAllFail.getCount(),
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
	private final class RequestHandler
	implements HttpAsyncRequestHandler<HttpRequest> {
		//
		final Map<String, WSObjectMock> sharedStorage;
		//
		private final int ringOffsetRadix = runTimeConfig.getInt(
			"storage.mock.data.offset.radix"
		);
		//
		public RequestHandler(final Map<String, WSObjectMock> sharedStorage) {
			super();
			this.sharedStorage = sharedStorage;
		}
		//
		@Override
		public HttpAsyncRequestConsumer<HttpRequest> processRequest(
			final HttpRequest request, final HttpContext context
		) throws HttpException, IOException {
			return new DummyAsyncRequestConsumer(runTimeConfig);
		}
		//
		@Override
		public final void handle(
			final HttpRequest request, final HttpAsyncExchange httpexchange,
			final HttpContext context
		){
			final HttpResponse response = httpexchange.getResponse();
			//HttpCoreContext coreContext = HttpCoreContext.adapt(context);
			String method = request.getRequestLine().getMethod().toLowerCase(Locale.ENGLISH);
			String dataID = "";
			//Get data Id
			try {
				final String[] requestUri = request.getRequestLine().getUri().split("/");
				if(requestUri.length >= 3) {
					dataID = requestUri[2];
				} else {
					method = "head";
				}
			} catch(final NumberFormatException e) {
				response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				TraceLogger.failure(
					LOG, Level.WARN, e,
					String.format("Unexpected object id format: \"%s\"", dataID)
				);
			} catch(final ArrayIndexOutOfBoundsException e) {
				response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				TraceLogger.failure(LOG, Level.WARN, e,
					"Request URI is not correct. Data object ID doesn't exist in request URI");
			}
			//
			switch(method) {
				case ("put"):
					doPut(request, response, dataID);
					break;
				case ("head"):
					doHead(response);
					break;
				case ("get"):
					doGet(response, dataID);
					break;
				case ("delete"):
					doDelete(response);
					break;
			}
			httpexchange.submitResponse(new BasicAsyncResponseProducer(response));
		}
		//
		private void doGet(final HttpResponse response, final String dataID){
			LOG.trace(Markers.MSG, " Request  method Get ");
			response.setStatusCode(HttpStatus.SC_OK);
			if(sharedStorage.containsKey(dataID)) {
				LOG.trace(Markers.MSG, "   Send data object ", dataID);
				final WSObjectMock object = sharedStorage.get(dataID);
				response.setEntity(object);
				LOG.trace(Markers.MSG, "   Response: OK");
				counterAllSucc.inc();
				counterGetSucc.inc();
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
		private void doPut(
			final HttpRequest request, final HttpResponse response, final String dataID
		){
			try {
				LOG.trace(Markers.MSG, " Request  method Put ");
				response.setStatusCode(HttpStatus.SC_OK);
				WSObjectMock dataObject = null;
				final HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				final long bytes = entity.getContentLength();
				//create data object or get it for append or update
				if (sharedStorage.containsKey(dataID)) {
					dataObject = sharedStorage.get(dataID);
				} else {
					final long offset;
					if (ringOffsetRadix == 0x40) { // base64
						offset = ByteBuffer
							.allocate(Long.SIZE / Byte.SIZE)
							.put(Base64.decodeBase64(dataID))
							.getLong(0);
					} else if (ringOffsetRadix > 1 && ringOffsetRadix <= Character.MAX_RADIX) {
						offset = Long.valueOf(dataID, ringOffsetRadix);
					} else {
						throw new HttpException(
							String.format(
								"Unsupported data ring offset radix: %d", ringOffsetRadix
							)
						);
					}
					dataObject = new BasicWSObjectMock(dataID, offset, bytes);
				}
				LOG.debug(Markers.DATA_LIST, dataObject.toString());
				synchronized (sharedStorage) {
					sharedStorage.put(dataID, dataObject);
				}
				counterAllSucc.inc();
				counterPutSucc.inc();
				putBW.mark(bytes);
				allBW.mark(bytes);
				putTP.mark();
				allTP.mark();
			} catch (final HttpException e){
				response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				TraceLogger.failure(LOG, Level.ERROR, e, "Put method failure");
				counterAllFail.inc();
				counterPutFail.inc();
			}
		}
		//
		private void doHead(final HttpResponse response){
			LOG.trace(Markers.MSG, " Request  method Head ");
			response.setStatusCode(HttpStatus.SC_OK);
			counterAllSucc.inc();
			counterHeadSucc.inc();
		}
		//
		private void doDelete(final HttpResponse response){
			LOG.trace(Markers.MSG, " Request  method Delete ");
			response.setStatusCode(HttpStatus.SC_OK);
			counterAllSucc.inc();
			counterDeleteSucc.inc();
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
			final NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory,
			final int port
		) throws IOReactorException {
			this.port = port;
			ioEventDispatch = new DefaultHttpServerIODispatch(protocolHandler, connFactory);
			// Set I/O reactor defaults
			final RunTimeConfig localRunTimeConfig = Main.RUN_TIME_CONFIG.get();
			final IOReactorConfig config = IOReactorConfig.custom()
				.setIoThreadCount(localRunTimeConfig.getInt("storage.mock.iothreads.persocket"))
				.setSoTimeout(localRunTimeConfig.getSocketTimeOut())
				.setConnectTimeout(localRunTimeConfig.getConnTimeOut())
				.build();
			// Create server-side I/O reactor
			ioReactor = new DefaultListeningIOReactor(config);
		}
		//
		@Override
		public void run() {
			try {
				// Listen of the given port
				ioReactor.listen(new InetSocketAddress(port));
				// Ready to go!
				ioReactor.execute(ioEventDispatch);
			} catch (final InterruptedIOException ex) {
				TraceLogger.failure(LOG, Level.DEBUG, ex, "Interrupted");
			} catch (final IOReactorException ex) {
				TraceLogger.failure(LOG, Level.ERROR, ex, "I/O reactor failure");
			} catch (final IOException ex) {
				TraceLogger.failure(LOG, Level.ERROR, ex, "I/O failure");
			}
		}
	}
}
