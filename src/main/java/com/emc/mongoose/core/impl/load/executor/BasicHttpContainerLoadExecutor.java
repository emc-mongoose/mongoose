package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.IoWorker;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.http.conn.pool.HttpConnPool;
import com.emc.mongoose.common.net.http.conn.pool.FixedRouteSequencingConnPool;
import com.emc.mongoose.common.net.http.request.HostHeaderSetter;
//
import com.emc.mongoose.common.net.http.BasicSslSetupHandler;
import com.emc.mongoose.common.net.ssl.SslContext;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
import com.emc.mongoose.core.api.io.task.IoTask;
import com.emc.mongoose.core.api.io.task.HttpContainerIoTask;
import com.emc.mongoose.core.api.io.task.HttpIoTask;
import com.emc.mongoose.core.api.load.executor.HttpContainerLoadExecutor;
//
import com.emc.mongoose.core.impl.io.task.BasicHttpContainerTask;
import com.emc.mongoose.core.impl.load.tasks.HttpClientRunTask;
//
import org.apache.http.ExceptionLogger;
import org.apache.http.HttpHost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.DefaultNHttpClientConnectionFactory;
import org.apache.http.impl.nio.SSLNHttpClientConnectionFactory;
import org.apache.http.impl.nio.pool.BasicNIOPoolEntry;
import org.apache.http.nio.NHttpConnectionFactory;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestUserAgent;
//
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.pool.BasicNIOConnFactory;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.pool.NIOConnFactory;
import org.apache.http.nio.protocol.HttpAsyncRequestExecutor;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.util.DirectByteBufferAllocator;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_HI;
import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_LO;
import static com.emc.mongoose.common.conf.enums.LoadType.READ;

/**
 Created by kurila on 20.10.15.
 */
public class BasicHttpContainerLoadExecutor<T extends HttpDataItem, C extends Container<T>>
extends LoadExecutorBase<C>
implements HttpContainerLoadExecutor<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final LoadType loadType;
	private final HttpProcessor httpProcessor;
	private final HttpAsyncRequester client;
	private final ConnectingIOReactor ioReactor;
	private final Map<HttpHost, HttpConnPool<HttpHost, BasicNIOPoolEntry>> connPoolMap;
	private final HttpRequestConfig<T, C> httpReqConfigCopy;
	private final boolean isPipeliningEnabled;
	//
	public BasicHttpContainerLoadExecutor(
		final AppConfig appConfig, final HttpRequestConfig<T, C> reqConfig, final String[] addrs,
		final int threadCount, final Input<C> itemInput, final long countLimit,
		final long sizeLimit, final float rateLimit
	) throws ClassCastException {
		super(appConfig, reqConfig, addrs, threadCount, itemInput, countLimit, sizeLimit, rateLimit);
		//
		this.loadType = reqConfig.getLoadType();
		httpReqConfigCopy = (HttpRequestConfig<T, C>) ioConfigCopy;
		isPipeliningEnabled = httpReqConfigCopy.getPipelining();
		//
		if(READ.equals(loadType)) {
			reqConfig.setBuffSize(BUFF_SIZE_HI);
		} else {
			reqConfig.setBuffSize(BUFF_SIZE_LO);
		}
		//
		final String userAgent = appConfig.getRunName() + "/" + appConfig.getRunVersion();
		//
		httpProcessor = HttpProcessorBuilder
			.create()
			.add(new HostHeaderSetter())
			.add(new RequestConnControl())
			.add(new RequestUserAgent(userAgent))
			//.add(new RequestExpectContinue(true))
			.add(new RequestContent(false))
			.add(httpReqConfigCopy)
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
		final long timeOutMs = TimeUnit.SECONDS.toMillis(appConfig.getLoadLimitTime());
		final IOReactorConfig.Builder ioReactorConfigBuilder = IOReactorConfig
			.custom()
			.setIoThreadCount(ThreadUtil.getWorkerCount())
			.setBacklogSize(appConfig.getNetworkSocketBindBacklogSize())
			.setInterestOpQueued(appConfig.getNetworkSocketInterestOpQueued())
			.setSelectInterval(appConfig.getNetworkSocketSelectInterval())
			.setShutdownGracePeriod(appConfig.getNetworkSocketTimeoutMilliSec())
			.setSoKeepAlive(appConfig.getNetworkSocketKeepAlive())
			.setSoLinger(appConfig.getNetworkSocketLinger())
			.setSoReuseAddress(appConfig.getNetworkSocketReuseAddr())
			.setSoTimeout(appConfig.getNetworkSocketTimeoutMilliSec())
			.setTcpNoDelay(appConfig.getNetworkSocketTcpNoDelay())
			.setRcvBufSize(BUFF_SIZE_LO)
			.setSndBufSize(BUFF_SIZE_LO)
			.setConnectTimeout(
				timeOutMs > 0 && timeOutMs < Integer.MAX_VALUE ? (int) timeOutMs : Integer.MAX_VALUE
			);
		//
		final NHttpClientEventHandler reqExecutor = new HttpAsyncRequestExecutor();
		//
		final ConnectionConfig connConfig = ConnectionConfig
			.custom()
			.setBufferSize(BUFF_SIZE_LO)
			.setFragmentSizeHint(0)
			.build();
		final IOEventDispatch ioEventDispatch = new DefaultHttpClientIODispatch(
			reqExecutor, connConfig
		);
		//
		final IoWorker.Factory ioWorkerFactory = new IoWorker.Factory(getName());
		try {
			ioReactor = new DefaultConnectingIOReactor(
				ioReactorConfigBuilder.build(), ioWorkerFactory
			);
		} catch(final IOReactorException e) {
			throw new IllegalStateException("Failed to build the I/O reactor", e);
		}
		//
		final NHttpConnectionFactory<? extends NHttpClientConnection>
			plainConnFactory = new DefaultNHttpClientConnectionFactory(
			null, null, DirectByteBufferAllocator.INSTANCE, connConfig
		);
		final NHttpConnectionFactory<? extends NHttpClientConnection> sslConnFactory;
		if(httpReqConfigCopy.getSslFlag()) {
			sslConnFactory = new SSLNHttpClientConnectionFactory(
				SslContext.INSTANCE, BasicSslSetupHandler.INSTANCE, null, null,
				DirectByteBufferAllocator.INSTANCE, connConfig
			);
		} else {
			sslConnFactory = null;
		}
		final NIOConnFactory<HttpHost, NHttpClientConnection>
			connFactory = new BasicNIOConnFactory(plainConnFactory, sslConnFactory);
		//
		connPoolMap = new HashMap<>(storageNodeCount);
		HttpHost nextRoute;
		HttpConnPool<HttpHost, BasicNIOPoolEntry> nextConnPool;
		for(int i = 0; i < storageNodeCount; i ++) {
			nextRoute = httpReqConfigCopy.getNodeHost(addrs[i]);
			nextConnPool = new FixedRouteSequencingConnPool(
				ioReactor, nextRoute, connFactory,
				timeOutMs > 0 && timeOutMs < Integer.MAX_VALUE ?
					(int) timeOutMs : Integer.MAX_VALUE,
				batchSize
			);
			nextConnPool.setDefaultMaxPerRoute(threadCount);
			nextConnPool.setMaxTotal(threadCount);
			connPoolMap.put(nextRoute, nextConnPool);
		}
		//
		mgmtTasks.add(new HttpClientRunTask(ioEventDispatch, ioReactor));
	}
	//
	@Override
	protected HttpContainerIoTask<T, C> getIoTask(final C item, final String nextNodeAddr) {
		return new BasicHttpContainerTask<>(item, nextNodeAddr, httpReqConfigCopy);
	}
	//
	@Override
	protected void interruptActually() {
		try {
			super.interruptActually();
		} finally {
			for(final HttpConnPool<HttpHost, BasicNIOPoolEntry> nextConnPool : connPoolMap.values()) {
				try {
					nextConnPool.closeExpired();
					LOG.debug(
						Markers.MSG, "{}: closed expired (if any) connections in the pool", getName()
					);
				} catch(final IllegalStateException e) {
					LogUtil.exception(
						LOG, Level.INFO, e,
						"{}: failed to closed expired connections in the pool", getName()
					);
				}
				try {
					nextConnPool.closeIdle(1, TimeUnit.MILLISECONDS);
					LOG.debug(
						Markers.MSG, "{}: closed idle connections (if any) in the pool", getName()
					);
				} catch(final IllegalStateException e) {
					LogUtil.exception(
						LOG, Level.INFO, e,
						"{}: failed to closed expired connections in the pool", getName()
					);
				} finally {
					try {
						nextConnPool.shutdown(1);
						LOG.debug(Markers.MSG, "{}: connection pool has been shut down", getName());
					} catch(final IOException e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "{}: connection pool shutdown failure", getName()
						);
					}
				}
			}
			//
			try {
				ioReactor.shutdown();
				LOG.debug(Markers.MSG, "{}: I/O reactor has been shut down", getName());
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "{}: failed to shut down the I/O reactor", getName()
				);
			}
		}
	}
	//
	@Override
	public final <A extends IoTask<C>> Future<A> submitTask(final A ioTask)
	throws RejectedExecutionException {
		//
		final HttpIoTask wsIoTask = (HttpIoTask) ioTask;
		final HttpConnPool<HttpHost, BasicNIOPoolEntry>
			connPool = connPoolMap.get(wsIoTask.getTarget());
		if(connPool.isShutdown()) {
			throw new RejectedExecutionException("Connection pool is shut down");
		}
		//
		final Future futureResult;
		try {
			futureResult = client.execute(wsIoTask, wsIoTask, connPool, wsIoTask, futureCallback);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "I/O task #{} has been submitted for execution", ioTask.hashCode()
				);
			}
		} catch(final Exception e) {
			throw new RejectedExecutionException(e);
		}
		return futureResult;
	}
	//
	private final FutureCallback<HttpContainerIoTask<T, C>>
		futureCallback = new FutureCallback<HttpContainerIoTask<T, C>>() {
			@Override
			public final void completed(final HttpContainerIoTask<T, C> ioTask) {
				try {
					ioTaskCompleted(ioTask);
				} catch(final RemoteException ignored) {
				}
			}
			//
			public final void cancelled() {
				ioTaskCancelled(1);
			}
			//
			public final void failed(final Exception e) {
				ioTaskFailed(1, e);
			}
		};
	//
	@Override
	public final <A extends IoTask<C>> int submitTasks(
		final List<A> ioTasks, final int from, final int to
	) throws RemoteException, RejectedExecutionException {
		int n = 0;
		if(isPipeliningEnabled) {
			if(ioTasks.size() > 0) {
				final List<HttpContainerIoTask<T, C>> wsIOTasks = (List<HttpContainerIoTask<T, C>>) ioTasks;
				final HttpContainerIoTask<T, C> anyTask = wsIOTasks.get(0);
				final HttpHost tgtHost = anyTask.getTarget();
				if(
					null == client.executePipelined(
						tgtHost, wsIOTasks, wsIOTasks, connPoolMap.get(tgtHost),
						HttpCoreContext.create(), new BatchFutureCallback(wsIOTasks)
					)
					) {
					return 0;
				}
			}
		} else {
			for(int i = from; i < to; i ++) {
				if(null != submitTask(ioTasks.get(i))) {
					n ++;
				} else {
					break;
				}
			}
		}
		return n;
	}
	//
	private final class BatchFutureCallback
	implements FutureCallback<List<HttpContainerIoTask<T, C>>> {
		//
		private final List<HttpContainerIoTask<T, C>> tasks;
		//
		private BatchFutureCallback(final List<HttpContainerIoTask<T, C>> tasks) {
			this.tasks = tasks;
		}
		//
		@Override
		public final void completed(final List<HttpContainerIoTask<T, C>> result) {
			try {
				ioTaskCompletedBatch(result, 0, result.size());
			} catch(final RemoteException ignored) {
			}
		}
		//
		@Override
		public final void failed(final Exception e) {
			ioTaskFailed(tasks.size(), e);
		}
		//
		@Override
		public final void cancelled() {
			ioTaskCancelled(tasks.size());
		}
	}
}
