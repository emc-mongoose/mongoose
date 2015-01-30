package com.emc.mongoose.web.storagemock;
//
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.logging.TraceLogger;
import com.emc.mongoose.util.threading.WorkerFactory;
import com.emc.mongoose.web.api.impl.WSRequestConfigBase;
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
import org.apache.http.nio.NHttpServerConnection;
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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
//
/**
 * Created by olga on 28.01.15.
 */
public class HttpMockServer {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static Map<String, WSObjectMock> MAP_DATA_OBJECT = new ConcurrentHashMap<>();
	private final static Queue<String> QUEUE_DATA_ID = new ConcurrentLinkedQueue<>();
	private static int MAX_PAGE_SIZE;
	//
	public HttpMockServer(final RunTimeConfig runTimeConfig)
	throws IOException {
		// count of ports = Count of kernel-1 or 1.
		final int portCount = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
		LOG.info(Markers.MSG, " WSMock can listen {} ports.",portCount);
		final String apiName = runTimeConfig.getStorageApi();
		final int portStart = runTimeConfig.getInt("api." + apiName + ".port");
		MAX_PAGE_SIZE = (int) runTimeConfig.getDataPageSize();
		// Set up the HTTP protocol processor
		final HttpProcessor httpproc = HttpProcessorBuilder.create()
			.add(new ResponseDate())
			.add(new ResponseServer("Test/1.1"))
			.add(new ResponseContent())
			.add(new ResponseConnControl()).build();
		// Create request handler registry
		final UriHttpAsyncRequestHandlerMapper reqistry = new UriHttpAsyncRequestHandlerMapper();
		// Register the default handler for all URIs
		reqistry.register("*", new HttpMockHandler());
		final HttpAsyncService protocolHandler = new HttpAsyncService(httpproc, reqistry) {
			//
			@Override
			public void connected(final NHttpServerConnection conn) {
				super.connected(conn);
			}
			//
			@Override
			public void closed(final NHttpServerConnection conn) {
				super.closed(conn);
			}
		};
		final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(portCount, portCount, 0, TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<Runnable>(1000000), new WorkerFactory("workerWSMock"));
		// Create HTTP connection factory
		final NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory = new DefaultNHttpServerConnectionFactory(
			ConnectionConfig.DEFAULT);
		//
		for (int i = 0; i < portCount; i++){
			threadPool.execute(new WorkerTask(protocolHandler,connFactory,portStart+i));
		}
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(1000, TimeUnit.DAYS);
		} catch (final InterruptedException e) {
			// do nothing
		}
		// Create server-side I/O event dispatch
		LOG.info(Markers.MSG, "Shutdown");
	}
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
			HttpResponse response = httpexchange.getResponse();
			//HttpCoreContext coreContext = HttpCoreContext.adapt(context);
			String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
			//
			switch (method){
				case ("PUT"):
					doPut(request, response);
				case ("HEAD"):
					doHead(request, response);
				case ("GET"):
					doGet(request,response);
			}
			httpexchange.submitResponse(new BasicAsyncResponseProducer(response));
		}
	}
	//
	private void doGet(
		final HttpRequest request, final HttpResponse response
	) throws  HttpException,IOException {
		LOG.trace(Markers.MSG, " Request  method Get ");
		response.setStatusCode(HttpStatus.SC_OK);
		String dataID = "";
		try{
			dataID = request.getRequestLine().getUri().split("/")[2];
			if (MAP_DATA_OBJECT.containsKey(dataID)) {
				LOG.trace(Markers.MSG, "   Send data object ", dataID);
				final WSObjectMock object = MAP_DATA_OBJECT.get(dataID);
				response.setEntity(object);
				//object.writeTo(servletOutputStream);
				LOG.trace(Markers.MSG, "   Response: OK");
			} else {
				response.setStatusCode(HttpStatus.SC_NOT_FOUND);
				LOG.trace(Markers.ERR, String.format("No such object: \"%s\"", dataID));
			}
		}catch (final NumberFormatException e){
			response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			TraceLogger.failure(
				LOG, Level.WARN, e,
				String.format("Unexpected object id format: \"%s\"", dataID)
			);
		}catch (final ArrayIndexOutOfBoundsException e) {
			response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			TraceLogger.failure(LOG, Level.WARN, e, "Request URI is not correct. Data object ID doesn't exist in request URI");
		}
	}
	//
	private void doPut(
		final HttpRequest request, final HttpResponse response
	) throws  HttpException,IOException {
		LOG.trace(Markers.MSG, " Request  method Put ");
		response.setStatusCode(HttpStatus.SC_OK);
		WSObjectMock dataObject = null;
		String dataID = "";
		try{
			final HttpEntity entity =  ((HttpEntityEnclosingRequest)request).getEntity();
			final long bytes = EntityUtils.toByteArray(entity).length;
			dataID = request.getRequestLine().getUri().split("/")[2];
			System.out.println(dataID);
			//create data object or get it for append or update
			if(MAP_DATA_OBJECT.containsKey(dataID)) {
				dataObject = MAP_DATA_OBJECT.get(dataID);
			} else {
				final long offset = Long.valueOf(dataID, WSRequestConfigBase.RADIX);
				dataObject = new BasicWSObjectMock(dataID, offset, bytes);
			}
			//
			MAP_DATA_OBJECT.put(dataID, dataObject);
			//
		} catch (final IOException e) {
			response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			TraceLogger.failure(LOG, Level.WARN, e, "Input stream failed");
		}catch (final NumberFormatException e){
			response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			TraceLogger.failure(
				LOG, Level.WARN, e,
				String.format("Unexpected object id format: \"%s\"", dataID)
			);
		}catch (final ArrayIndexOutOfBoundsException e) {
			response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			TraceLogger.failure(LOG, Level.WARN, e, "Request URI is not correct. Data object ID doesn't exist in request URI");
		} catch (final Exception e) {
			response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			TraceLogger.failure(LOG, Level.WARN, e, "Ranges have not correct form.");
		}
	}
	//
	private void doHead(
		final HttpRequest request, final HttpResponse response
	) throws  HttpException,IOException {
		LOG.trace(Markers.MSG, " Request  method Head ");
		response.setStatusCode(HttpStatus.SC_OK);
	}
	///////////////////////////////////////////////////////////////////////////////////
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
				.setIoThreadCount(10)
				.setSoTimeout(3000)
				.setConnectTimeout(3000)
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
