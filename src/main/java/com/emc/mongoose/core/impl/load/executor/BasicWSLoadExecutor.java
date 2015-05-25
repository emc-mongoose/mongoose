package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
import com.emc.mongoose.common.http.RequestSharedHeaders;
import com.emc.mongoose.common.http.RequestTargetHost;
import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.task.WSIOTask;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import com.emc.mongoose.core.api.load.executor.WSLoadExecutor;
import com.emc.mongoose.core.api.load.model.Producer;
//
import com.emc.mongoose.core.impl.load.executor.util.DataObjectWorkerFactory;
import com.emc.mongoose.core.impl.load.model.BasicWSObjectGenerator;
import com.emc.mongoose.core.impl.load.model.FileProducer;
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.load.tasks.HttpClientRunTask;
//
import org.apache.http.ExceptionLogger;
import org.apache.http.HttpHost;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.message.HeaderGroup;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestUserAgent;
//
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.pool.BasicNIOConnFactory;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.pool.NIOConnFactory;
import org.apache.http.nio.protocol.HttpAsyncRequestExecutor;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 02.12.14.
 */
public class BasicWSLoadExecutor<T extends WSObject>
extends ObjectLoadExecutorBase<T>
implements WSLoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	;
	//
	private final HttpAsyncRequester client;
	private final ConnectingIOReactor ioReactor;
	private final BasicNIOConnPool connPool;
	private final Thread clientDaemon;
	private final WSRequestConfig<T> wsReqConfigCopy;
	//
	public BasicWSLoadExecutor(
		final RunTimeConfig runTimeConfig, final WSRequestConfig<T> reqConfig, final String[] addrs,
		final int connCountPerNode, final String listFile, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias, final float rateLimit,
		final int countUpdPerReq
	) {
		super(
			runTimeConfig, reqConfig, addrs, connCountPerNode, listFile, maxCount,
			sizeMin, sizeMax, sizeBias, rateLimit, countUpdPerReq
		);
		this.wsReqConfigCopy = (WSRequestConfig<T>) reqConfigCopy;
		//
		final int totalConnCount = connCountPerNode * storageNodeCount;
		final HeaderGroup sharedHeaders = wsReqConfigCopy.getSharedHeaders();
		final String userAgent = runTimeConfig.getRunName() + "/" + runTimeConfig.getRunVersion();
		//
		final HttpProcessor httpProcessor = HttpProcessorBuilder
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
					LogUtil.exception(LOG, Level.DEBUG, e, "HTTP client internal failure");
				}
			}
		);
		//
		final RunTimeConfig thrLocalConfig = RunTimeConfig.getContext();
		final int buffSize = wsReqConfigCopy.getBuffSize();
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
			.setRcvBufSize(IOTask.Type.READ.equals(loadType) ? buffSize : BUFF_SIZE_LO)
			.setSndBufSize(IOTask.Type.READ.equals(loadType) ? BUFF_SIZE_LO : buffSize)
			.setConnectTimeout(thrLocalConfig.getConnTimeOut());
		//
		final NHttpClientEventHandler reqExecutor = new HttpAsyncRequestExecutor();
		//
		final ConnectionConfig connConfig = ConnectionConfig
			.custom()
			.setBufferSize(BUFF_SIZE_LO)
			.setFragmentSizeHint(BUFF_SIZE_LO)
			.build();
		final IOEventDispatch ioEventDispatch = new DefaultHttpClientIODispatch(
			reqExecutor, connConfig
		);
		//
		try {
			ioReactor = new DefaultConnectingIOReactor(
				ioReactorConfigBuilder.build(),
				new DataObjectWorkerFactory(loadNum, wsReqConfigCopy.getAPI(), loadType, name)
			);
		} catch(final IOReactorException e) {
			throw new IllegalStateException("Failed to build the I/O reactor", e);
		}
		//
		//final NHttpMessageParserFactory<HttpResponse>
		//	respParserFactory = new DefaultHttpResponseParserFactory(
		//		null, new DefaultHttpResponseFactory()
		//	);
		final NIOConnFactory<HttpHost, NHttpClientConnection>
			connFactory = new BasicNIOConnFactory(
				/*null, null, respParserFactory, null,
				HeapByteBufferAllocator.INSTANCE, */connConfig
			);
		//
		connPool = new BasicNIOConnPool(
			ioReactor, connFactory, runTimeConfig.getConnPoolTimeOut()
		);
		connPool.setMaxTotal(totalConnCount);
		connPool.setDefaultMaxPerRoute(totalConnCount);
		//
		clientDaemon = new Thread(
			new HttpClientRunTask(ioEventDispatch, ioReactor),
			String.format("%s-webClientThread", getName())
		);
		clientDaemon.setDaemon(true);
	}
	//
	@Override
	public synchronized void start() {
		if(clientDaemon == null) {
			LOG.debug(LogUtil.ERR, "Not starting web load client due to initialization failures");
		} else {
			clientDaemon.start();
			super.start();
		}
	}
	//
	@Override
	public void close()
	throws IOException {
		try {
			super.close();
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Closing failure");
		}
		//
		clientDaemon.interrupt();
		LOG.debug(LogUtil.MSG, "Web storage client daemon \"{}\" interrupted", clientDaemon);
		if(connPool != null) {
			connPool.closeExpired();
			LOG.debug(LogUtil.MSG, "Closed expired (if any) connections in the pool");
			try {
				connPool.closeIdle(1, TimeUnit.MILLISECONDS);
				LOG.debug(LogUtil.MSG, "Closed idle connections (if any) in the pool");
			} finally {
				try {
					connPool.shutdown(1);
					LOG.debug(LogUtil.MSG, "Connection pool has been shut down");
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Connection pool shutdown failure");
				}
			}
		}
		//
		ioReactor.shutdown(1);
		LOG.debug(LogUtil.MSG, "I/O reactor has been shut down");
	}
	//
	@Override
	public final Future<IOTask.Status> submit(final IOTask<T> ioTask)
	throws RejectedExecutionException {
		//
		if(connPool.isShutdown()) {
			throw new RejectedExecutionException("Connection pool is shut down");
		}
		//
		final WSIOTask<T> wsTask = (WSIOTask<T>) ioTask;
		final Future<IOTask.Status> futureResult;
		try {
			futureResult = client.execute(wsTask, wsTask, connPool, wsTask, wsTask);
		} catch(final IllegalStateException e) {
			throw new RejectedExecutionException(e);
		}
		if(LOG.isTraceEnabled(LogUtil.MSG)) {
			LOG.trace(
				LogUtil.MSG, "I/O task #{} has been submitted for execution",
				wsTask.hashCode()
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
			LogUtil.exception(LOG, Level.FATAL, e, "Unexpected failure");
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to read the data items file \"{}\"", listFile
			);
		}
		return localProducer;
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected Producer<T> newDataProducer(
		final long maxCount, final long minObjSize, final long maxObjSize, final float objSizeBias
	) {
		return (Producer<T>) new BasicWSObjectGenerator<>(
			maxCount, minObjSize, maxObjSize, objSizeBias
		);
	}
}
