package com.emc.mongoose.storage.mock.impl.web;
// mongoose-common.jar
import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_LO;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.date.LowPrecisionDateGenerator;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-storage-mock.jar
import com.emc.mongoose.storage.mock.impl.base.ObjectStorageMockBase;
import com.emc.mongoose.storage.mock.impl.web.data.BasicWSObjectMock;
import com.emc.mongoose.storage.mock.impl.web.net.BasicSocketEventDispatcher;
import com.emc.mongoose.storage.mock.impl.web.request.APIRequestHandlerMapper;
import com.emc.mongoose.storage.mock.impl.web.net.BasicWSMockConnFactory;
//
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseServer;
//
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.NHttpConnectionFactory;
import org.apache.http.nio.protocol.HttpAsyncRequestHandlerMapper;
import org.apache.http.nio.protocol.HttpAsyncService;
import org.apache.http.nio.reactor.IOReactorException;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
/**
 * Created by olga on 28.01.15.
 */
public final class Cinderella
extends ObjectStorageMockBase<BasicWSObjectMock> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final BasicSocketEventDispatcher sockEvtDispatchers[] ;
	private final HttpAsyncService protocolHandler;
	private final NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory;
	//
	public Cinderella(final RunTimeConfig rtConfig)
	throws IOException {
		super(rtConfig, BasicWSObjectMock.class);
		sockEvtDispatchers = new BasicSocketEventDispatcher[rtConfig.getStorageMockHeadCount()];
		LOG.info(Markers.MSG, "Starting with {} heads", sockEvtDispatchers.length);
		// connection config
		final ConnectionConfig connConfig = ConnectionConfig
			.custom()
			.setBufferSize(BUFF_SIZE_LO)
			.build();
		connFactory = new BasicWSMockConnFactory(rtConfig, connConfig);
		// Set up the HTTP protocol processor
		final HttpProcessor httpProc = HttpProcessorBuilder.create()
			.add( // this is a date header generator below
				new HttpResponseInterceptor() {
					@Override
					public void process(
						final HttpResponse response, final HttpContext context
					) throws HttpException, IOException {
						response.setHeader(
							HTTP.DATE_HEADER, LowPrecisionDateGenerator.getDateText()
						);
					}
				}
			)
			.add( // user-agent header
				new ResponseServer(
					String.format(
						"%s/%s", Cinderella.class.getSimpleName(), rtConfig.getRunVersion()
					)
				)
			)
			.add(new ResponseContent())
			.add(new ResponseConnControl())
			.build();
		// Create request handler registry
		final HttpAsyncRequestHandlerMapper apiReqHandlerMapper = new APIRequestHandlerMapper<>(
			rtConfig, this
		);
		// Register the default handler for all URIs
		protocolHandler = new HttpAsyncService(httpProc, apiReqHandlerMapper);
	}
	//
	@Override
	protected final void startListening() {
		int nextPort;
		for(int i = 0; i < sockEvtDispatchers.length; i++) {
			nextPort = portStart + i;
			try {
				sockEvtDispatchers[i] = new BasicSocketEventDispatcher(
					rtConfig, protocolHandler, nextPort, connFactory, ioStats
				);
				sockEvtDispatchers[i].start();
			} catch(final IOReactorException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to start the head at port #{}", nextPort
				);
			}
		}
		if(sockEvtDispatchers.length > 1) {
			LOG.info(Markers.MSG, "Listening the ports {} .. {}",
				portStart, portStart + sockEvtDispatchers.length - 1);
		} else {
			LOG.info(Markers.MSG, "Listening the port {}", portStart);
		}
		//
	}
	//
	@Override
	protected final void await() {
		try {
			for(final BasicSocketEventDispatcher sockEvtDispatcher : sockEvtDispatchers) {
				if(sockEvtDispatcher != null) {
					sockEvtDispatcher.join();
				}
			}
		} catch(final InterruptedException e) {
			LOG.info(Markers.MSG, "Interrupting the Cinderella");
		}
	}
	//
	@Override
	public final void close()
	throws IOException {
		try {
			createConsumer.close();
			LOG.debug(Markers.MSG, "Create consumer closed successfully");
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "I/O failure on close");
		}
		//
		try {
			deleteConsumer.close();
			LOG.debug(Markers.MSG, "Delete consumer closed successfully");
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "I/O failure on close");
		}
		//
		for(final BasicSocketEventDispatcher sockEventDispatcher : sockEvtDispatchers) {
			if(sockEventDispatcher != null) {
				try {
					sockEventDispatcher.close();
					LOG.debug(
						Markers.MSG, "Socket event dispatcher \"{}\" closed successfully",
						sockEventDispatcher
					);
				} catch(final IOException e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "Closing socket event dispatcher \"{}\" failure",
						sockEventDispatcher
					);
				}
			}
		}
		//
		super.close();
	}
	//
}
