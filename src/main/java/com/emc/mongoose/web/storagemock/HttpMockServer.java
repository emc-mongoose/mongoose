package com.emc.mongoose.web.storagemock;
//
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.logging.TraceLogger;
import com.emc.mongoose.util.remote.ServiceUtils;
import com.emc.mongoose.util.threading.WorkerFactory;
//
import com.emc.mongoose.web.api.impl.WSRequestConfigBase;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.DefaultHttpServerIODispatch;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.DefaultNHttpServerConnectionFactory;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.NHttpConnectionFactory;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
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
import org.apache.http.util.EntityUtils;
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
	final NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory = new DefaultNHttpServerConnectionFactory(
		ConnectionConfig.DEFAULT);
	//
	private final static String NAME_SERVER = "StorageMock/0.1";
	private final static int BASE_16 = 16,
		BASE_36 = 36,
		BASE_64 = 64;
	private final int baseDataID;
	private final int portCount;
	private final int portStart;
	//
	private final RunTimeConfig runTimeConfig;
	//
	private final JmxReporter metricsReporter;
	//
	public HttpMockServer(final RunTimeConfig runTimeConfig)
	throws IOException {
		this.runTimeConfig = runTimeConfig;
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
		metricsReporter.start();
		//queue size for data object
		final int queueDataIdSize = runTimeConfig.getInt("wsmock.queue.dataobject.size");
		queueDataId = new ArrayBlockingQueue<>(queueDataIdSize);
		mapDataObject = new ConcurrentHashMap<>(queueDataIdSize);
		//
		baseDataID = runTimeConfig.getInt("wsmock.data.id.base");
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
		multiSocketSvc = Executors.newFixedThreadPool(portCount, new WorkerFactory("workerWSMock"));
	}

	@Override
	public void run() {
		for (int i = 0; i < portCount; i++){
			multiSocketSvc.submit(new WorkerTask(protocolHandler, connFactory, portStart + i));
			LOG.info(Markers.MSG, " WSMock can listen {} port.", portStart + i);
		}
		multiSocketSvc.shutdown();
		try {
			multiSocketSvc.awaitTermination(runTimeConfig.getRunTimeValue(), runTimeConfig.getRunTimeUnit());
		} catch (final InterruptedException e) {
			// do nothing
			LOG.info(Markers.MSG, "Interrupting the WSMock");
		} finally {
			metricsReporter.close();
		}
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
			return new BasicAsyncRequestConsumer();
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
		} else {
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
			LOG.trace(Markers.ERR, String.format("No such object: \"%s\"", dataID));
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
			final long bytes = EntityUtils.toByteArray(entity).length;
			//create data object or get it for append or update
			if(mapDataObject.containsKey(dataID)) {
				dataObject = mapDataObject.get(dataID);
			} else {
				long offset = 0;
				switch (baseDataID){
					case BASE_64:
						final byte dataIdBytes[] = Base64.decodeBase64(dataID);
						offset  = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).put(dataIdBytes).getLong(0);
						break;
					case BASE_36:
						offset = Long.valueOf(dataID, WSRequestConfigBase.RADIX);
						break;
					case BASE_16:
						offset = Long.valueOf(dataID, 0x10);
						break;
				}
				dataObject = new BasicWSObjectMock(dataID, offset, bytes);
			}
			try {
				synchronized (queueDataId) {
					if (!queueDataId.offer(dataID)) {
						LOG.trace(Markers.MSG, " Queue is full");
						mapDataObject.remove(queueDataId.remove());
						queueDataId.add(dataID);
					}
					mapDataObject.put(dataID, dataObject);
				}
			}catch (final IllegalStateException  e){
				//response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				TraceLogger.failure(LOG, Level.WARN, e, "Memory is full");
			}
		} catch (final IOException e) {
			response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			TraceLogger.failure(LOG, Level.WARN, e, "Input stream failed");
		}
	}
	//
	private void doHead(final HttpResponse response)
	throws  HttpException, IOException
	{
		LOG.trace(Markers.MSG, " Request  method Head ");
		response.setStatusCode(HttpStatus.SC_OK);
	}
	///////////////////////////////////////////////////////////////////////////////////////////////////
	//WorkerTask
	///////////////////////////////////////////////////////////////////////////////////////////////////
	class WorkerTask
	implements Runnable{
		//
		private HttpAsyncService protocolHandler;
		private NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory;
		private int port;
		//
		public WorkerTask(final HttpAsyncService protocolHandler,
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
