package com.emc.mongoose.storage.mock.impl.web;
// mongoose-common.jar
import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_LO;

import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.date.LowPrecisionDateGenerator;
import com.emc.mongoose.common.io.IOWorker;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-storage-mock.jar
import com.emc.mongoose.core.api.data.content.ContentSource;
import com.emc.mongoose.core.impl.data.content.ContentSourceBase;
import com.emc.mongoose.storage.mock.api.WSMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
import com.emc.mongoose.storage.mock.impl.base.ObjectStorageMockBase;
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
public final class Cinderella<T extends WSObjectMock>
extends ObjectStorageMockBase<T>
implements WSMock<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final BasicSocketEventDispatcher sockEvtDispatchers[] ;
	private final HttpAsyncService protocolHandler;
	private final NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory;
	private final int portStart;
	private final ContentSource contentSrc;
	//
	public Cinderella(final RunTimeConfig rtConfig)
	throws IOException {
		this(rtConfig, rtConfig.getStorageMockWorkersPerSocket());
	}
	//
	private Cinderella(final RunTimeConfig rtConfig, final int ioThreadCount)
	throws IOException {
		this(
			rtConfig.getStorageMockHeadCount(),
			ioThreadCount > 0 ? ioThreadCount : ThreadUtil.getWorkerCount(),
			rtConfig.getApiTypePort(rtConfig.getApiName()),
			rtConfig.getStorageMockCapacity(),
			rtConfig.getStorageMockContainerCapacity(),
			rtConfig.getStorageMockContainerCountLimit(),
			rtConfig.getDataSrcFPath(),
			rtConfig.getLoadMetricsPeriodSec(),
			rtConfig.getFlagServeJMX(),
			rtConfig.getStorageMockMinConnLifeMilliSec(),
			rtConfig.getStorageMockMaxConnLifeMilliSec()
		);
	}
	//
	public Cinderella(
		final int headCount, final int ioThreadCount, final int portStart,
		final int storageCapacity, final int containerCapacity, final int containerCountLimit,
		final String dataSrcPath, final int metricsPeriodSec, final boolean jmxServeFlag,
	    final int minConnLifeMilliSec, final int maxConnLifeMilliSec
	) throws IOException {
		super(
			(Class<T>) BasicWSObjectMock.class,
			storageCapacity, containerCapacity, containerCountLimit,
			headCount * ioThreadCount,
			dataSrcPath, metricsPeriodSec, jmxServeFlag
		);
		this.portStart = portStart;
		sockEvtDispatchers = new BasicSocketEventDispatcher[headCount];
		LOG.info(Markers.MSG, "Starting with {} heads", sockEvtDispatchers.length);
		// connection config
		final ConnectionConfig connConfig = ConnectionConfig
			.custom()
			.setBufferSize(BUFF_SIZE_LO)
			.setFragmentSizeHint(0)
			.build();
		connFactory = new BasicWSMockConnFactory(
			connConfig, minConnLifeMilliSec, maxConnLifeMilliSec
		);
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
					Cinderella.class.getSimpleName() + "/" +
					RunTimeConfig.getContext().getRunVersion()
				)
			)
			.add(new ResponseContent())
			.add(new ResponseConnControl())
			.build();
		// Create request handler registry
		final HttpAsyncRequestHandlerMapper apiReqHandlerMapper = new APIRequestHandlerMapper<>(
			RunTimeConfig.getContext(), this
		);
		// Register the default handler for all URIs
		protocolHandler = new HttpAsyncService(httpProc, apiReqHandlerMapper);
		//
		contentSrc = ContentSourceBase.getDefault();
	}
	//
	@Override
	protected final void startListening() {
		int nextPort;
		for(int i = 0; i < sockEvtDispatchers.length; i++) {
			nextPort = portStart + i;
			try {
				sockEvtDispatchers[i] = new BasicSocketEventDispatcher(
					RunTimeConfig.getContext(), protocolHandler, nextPort, connFactory, ioStats
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
	protected final T newDataObject(final String id, final long offset, final long size) {
		return (T) new BasicWSObjectMock(id, offset, size, contentSrc);
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
}
