package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
import com.emc.mongoose.common.http.RequestSharedHeaders;
import com.emc.mongoose.common.http.RequestTargetHost;
import com.emc.mongoose.common.logging.Markers;
import com.emc.mongoose.common.logging.TraceLogger;
//
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.task.WSIOTask;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import com.emc.mongoose.core.api.load.executor.WSLoadExecutor;
import com.emc.mongoose.core.api.load.model.Producer;
//
import com.emc.mongoose.core.impl.load.model.BasicWSDataGenerator;
import com.emc.mongoose.core.impl.load.model.FileProducer;
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.load.tasks.HttpClientRunTask;
//
import org.apache.http.ExceptionLogger;
import org.apache.http.HttpHost;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.pool.BasicNIOConnFactory;
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.HeaderGroup;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.pool.NIOConnFactory;
import org.apache.http.nio.protocol.HttpAsyncRequestExecutor;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestUserAgent;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 02.12.14.
 */
public class BasicWSLoadExecutor<T extends WSObject>
extends ObjectLoadExecutorBase<T>
implements WSLoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final HttpAsyncRequester client;
	private final BasicNIOConnPool connPool;
	private final Thread clientThread;
	//
	public BasicWSLoadExecutor(
		final RunTimeConfig runTimeConfig, final WSRequestConfig<T> reqConfig, final String[] addrs,
		final int connCountPerNode, final String listFile, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias, final int countUpdPerReq
	) {
		super(
			runTimeConfig, reqConfig, addrs, connCountPerNode, listFile, maxCount,
			sizeMin, sizeMax, sizeBias, countUpdPerReq
		);
		//
		final int totalConnCount = connCountPerNode * storageNodeCount;
		final HeaderGroup sharedHeaders = WSRequestConfig.class.cast(reqConfigCopy)
			.getSharedHeaders();
		final String userAgent = runTimeConfig.getRunName() + "/" + runTimeConfig.getRunVersion();
		//
		final HttpProcessor httpProcessor= HttpProcessorBuilder
			.create()
			.add(new RequestSharedHeaders(sharedHeaders))
			.add(new RequestTargetHost())
			.add(new RequestConnControl())
			.add(new RequestUserAgent(userAgent))
			//.add(new RequestExpectContinue(true))
			.add(new RequestContent(false))
			.build();
		client = new HttpAsyncRequester(
			httpProcessor, DefaultConnectionReuseStrategy.INSTANCE,
			new ExceptionLogger() {
				@Override
				public final void log(final Exception e) {
					TraceLogger.failure(LOG, Level.DEBUG, e, "HTTP client internal failure");
				}
			}
		);
		//
		final RunTimeConfig thrLocalConfig = RunTimeConfig.getContext();
		final ConnectionConfig connConfig = ConnectionConfig
			.custom()
			.setBufferSize((int) thrLocalConfig.getDataPageSize())
			.build();
		final IOReactorConfig.Builder ioReactorConfigBuilder = IOReactorConfig
			.custom()
			.setIoThreadCount(totalConnCount)
			.setBacklogSize((int) thrLocalConfig.getSocketBindBackLogSize())
			.setInterestOpQueued(thrLocalConfig.getSocketInterestOpQueued())
			.setSelectInterval(thrLocalConfig.getSocketSelectInterval())
			.setShutdownGracePeriod(thrLocalConfig.getSocketTimeOut())
			.setSoKeepAlive(thrLocalConfig.getSocketKeepAliveFlag())
			.setSoLinger(thrLocalConfig.getSocketLinger())
			.setSoReuseAddress(thrLocalConfig.getSocketReuseAddrFlag())
			.setSoTimeout(thrLocalConfig.getSocketTimeOut())
			.setTcpNoDelay(thrLocalConfig.getSocketTCPNoDelayFlag())
			.setRcvBufSize((int) thrLocalConfig.getDataPageSize())
			.setSndBufSize((int) thrLocalConfig.getDataPageSize())
			.setConnectTimeout(thrLocalConfig.getConnTimeOut());
		//
		final NHttpClientEventHandler reqExecutor = new HttpAsyncRequestExecutor();
		//
		final IOEventDispatch ioEventDispatch = new DefaultHttpClientIODispatch(
			reqExecutor, connConfig
		);
		//
		ConnectingIOReactor ioReactor = null;
		try {
			ioReactor = new DefaultConnectingIOReactor(
				ioReactorConfigBuilder.build(),
				new NamingWorkerFactory(String.format("IOWorker<%s>", getName()))
			);
		} catch(final IOReactorException e) {
			TraceLogger.failure(LOG, Level.FATAL, e, "Failed to build I/O reactor");
		}
		//
		final NIOConnFactory<HttpHost, NHttpClientConnection>
			connFactory = new BasicNIOConnFactory(connConfig);
		if(ioReactor != null) {
			//
			connPool = new BasicNIOConnPool(
				ioReactor, connFactory, runTimeConfig.getConnPoolTimeOut()
			);
			connPool.setMaxTotal(totalConnCount);
			connPool.setDefaultMaxPerRoute(totalConnCount);
			clientThread = new Thread(
				new HttpClientRunTask<>(ioEventDispatch, ioReactor),
				String.format("%s-webClientThread", getName())
			);
		} else {
			connPool = null;
			clientThread = null;
		}
	}
	//
	@Override
	public synchronized void start() {
		if(clientThread== null) {
			LOG.debug(Markers.ERR, "Not starting web load client due to initialization failures");
		} else {
			clientThread.start();
			super.start();
		}
	}
	//
	private final static int SHUTDOWN_TIMEOUT_MILLISEC = 1000;
	@Override
	public void close()
	throws IOException {
		try {
			super.close();
		} catch(final IOException e) {
			TraceLogger.failure(LOG, Level.WARN, e, "Closing failure");
		}
		//
		LOG.debug(Markers.MSG, "Going to close the web storage client");
		//
		clientThread.interrupt();
		//
		if(connPool != null) {
			connPool.closeExpired();
			try {
				connPool.closeIdle(
					SHUTDOWN_TIMEOUT_MILLISEC, TimeUnit.MILLISECONDS
				);
			} finally {
				try {
					connPool.shutdown(SHUTDOWN_TIMEOUT_MILLISEC);
				} catch(final IOException e) {
					TraceLogger.failure(
						LOG, Level.WARN, e, "Connection pool shutdown failure"
					);
				}
			}
		}
		//
		LOG.debug(Markers.MSG, "Closed web storage client");
	}
	//
	@Override
	public final Future<IOTask.Status> submit(final IOTask<T> ioTask) {
		final WSIOTask<T> wsTask = (WSIOTask<T>) ioTask;
		Future<WSIOTask.Status> futureResult = null;
		try {
			futureResult = client.execute(wsTask, wsTask, connPool, wsTask.getHttpContext());
		} catch(final IllegalStateException e) {
			TraceLogger.failure(
				LOG, Level.WARN, e, "Failed to submit the HTTP request for execution"
			);
		}
		return futureResult;
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected Producer<T> newFileBasedProducer(final long maxCount, final String listFile) {
		Producer<T> localProducer = null;
		try {
			localProducer = (Producer<T>) new FileProducer<>(
				maxCount, listFile, BasicWSObject.class
			);
		} catch(final NoSuchMethodException e) {
			TraceLogger.failure(LOG, Level.FATAL, e, "Unexpected failure");
		} catch(final IOException e) {
			TraceLogger.failure(
				LOG, Level.ERROR, e,
				String.format("Failed to read the data items file \"%s\"", listFile)
			);
		}
		return localProducer;
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected Producer<T> newDataProducer(
		final long maxCount, final long minObjSize, final long maxObjSize, final float objSizeBias
	) {
		return (Producer<T>) new BasicWSDataGenerator<>(
			maxCount, minObjSize, maxObjSize, objSizeBias
		);
	}
}
