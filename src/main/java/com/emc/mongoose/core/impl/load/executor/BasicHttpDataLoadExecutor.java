package com.emc.mongoose.core.impl.load.executor;
// mongoose-common.jar
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.DataRangesConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.io.IoWorker;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.http.conn.pool.HttpConnPool;
import com.emc.mongoose.common.net.http.conn.pool.FixedRouteSequencingConnPool;
import com.emc.mongoose.common.net.http.request.HostHeaderSetter;
import com.emc.mongoose.common.log.LogUtil;
// mongoose-core-api.jar
import com.emc.mongoose.common.net.http.BasicSslSetupHandler;
import com.emc.mongoose.common.net.ssl.SslContext;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.io.task.IoTask;
import com.emc.mongoose.core.api.io.task.HttpDataIoTask;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
import com.emc.mongoose.core.api.load.executor.HttpDataLoadExecutor;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.io.task.BasicHttpDataIoTask;
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
//
import org.apache.http.nio.util.DirectByteBufferAllocator;
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
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_LO;
import static com.emc.mongoose.common.conf.enums.LoadType.DELETE;
import static com.emc.mongoose.common.conf.enums.LoadType.READ;

/**
 Created by kurila on 02.12.14.
 */
public class BasicHttpDataLoadExecutor<T extends HttpDataItem>
extends DataLoadExecutorBase<T>
implements HttpDataLoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final HttpProcessor httpProcessor;
	protected final HttpAsyncRequester client;
	protected final ConnectingIOReactor ioReactor;
	protected final Map<HttpHost, HttpConnPool<HttpHost, BasicNIOPoolEntry>> connPoolMap;
	private final HttpRequestConfig<T, Container<T>> httpReqConfigCopy;
	private final boolean isPipeliningEnabled;
	//
	public BasicHttpDataLoadExecutor(
		final AppConfig appConfig, final HttpRequestConfig<T, ? extends Container<T>> reqConfig,
		final String[] addrs, final int threadCount, final Input<T> itemInput,
		final long countLimit, final long sizeLimit, final float rateLimit,
		final SizeInBytes sizeConfig, final DataRangesConfig rangesConfig,
		final HttpProcessor httpProcessor, final HttpAsyncRequester client,
		final ConnectingIOReactor ioReactor,
		final Map<HttpHost, HttpConnPool<HttpHost, BasicNIOPoolEntry>> connPoolMap
	) {
		super(
			appConfig, reqConfig, addrs, threadCount, itemInput, countLimit, sizeLimit, rateLimit,
			sizeConfig, rangesConfig
		);
		this.httpProcessor = httpProcessor;
		this.client = client;
		this.ioReactor = ioReactor;
		this.connPoolMap = connPoolMap;
		httpReqConfigCopy = (HttpRequestConfig<T, Container<T>>) ioConfig;
		isPipeliningEnabled = httpReqConfigCopy.getPipelining();
	}
	//
	public BasicHttpDataLoadExecutor(
		final AppConfig appConfig, final HttpRequestConfig<T, ? extends Container<T>> reqConfig,
		final String[] addrs, final int threadCount, final Input<T> itemInput,
		final long countLimit, final long sizeLimit, final float rateLimit,
		final SizeInBytes sizeConfig, final DataRangesConfig rangesConfig
	) {
		super(
			appConfig, reqConfig, addrs, threadCount, itemInput, countLimit, sizeLimit, rateLimit,
			sizeConfig, rangesConfig
		);
		httpReqConfigCopy = (HttpRequestConfig<T, Container<T>>) ioConfig;
		isPipeliningEnabled = httpReqConfigCopy.getPipelining();
		//
		httpProcessor = HttpProcessorBuilder
			.create()
			.add(new HostHeaderSetter())
			.add(new RequestConnControl())
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
		final int buffSize = httpReqConfigCopy.getBuffSize();
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
			.setRcvBufSize(READ.equals(loadType) ? buffSize : BUFF_SIZE_LO)
			.setSndBufSize(
				READ.equals(loadType) || DELETE.equals(loadType) ? BUFF_SIZE_LO : buffSize
			)
			.setConnectTimeout(
				timeOutMs > 0 && timeOutMs < Integer.MAX_VALUE ? (int) timeOutMs : Integer.MAX_VALUE
			);
		//
		final NHttpClientEventHandler reqExecutor = new HttpAsyncRequestExecutor();
		//
		final ConnectionConfig connConfig = ConnectionConfig
			.custom()
			.setBufferSize(buffSize)
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
				batchSize, threadCount, threadCount
			);
			connPoolMap.put(nextRoute, nextConnPool);
		}
		//
		mgmtTasks.add(new HttpClientRunTask(ioEventDispatch, ioReactor));
	}
	//
	@Override
	protected HttpDataIoTask<T> getIoTask(final T item, final String nodeAddr) {
		return new BasicHttpDataIoTask<>(item, nodeAddr, httpReqConfigCopy);
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
						LOG, Level.DEBUG, e,
						"{}: failed to close the expired connections in the pool", getName()
					);
				}
				try {
					nextConnPool.closeIdle(1, TimeUnit.MILLISECONDS);
					LOG.debug(
						Markers.MSG, "{}: closed idle connections (if any) in the pool", getName()
					);
				} catch(final IllegalStateException e) {
					LogUtil.exception(
						LOG, Level.DEBUG, e,
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
	@Override @SuppressWarnings("unchecked")
	public <A extends IoTask<T>> Future submitTask(final A ioTask)
	throws RejectedExecutionException {
		final HttpDataIoTask<T> wsIoTask = (HttpDataIoTask<T>) ioTask;
		final HttpHost tgtHost = wsIoTask.getTarget();
		final HttpConnPool<HttpHost, BasicNIOPoolEntry>
			connPool = connPoolMap.get(tgtHost);
		if(connPool.isShutdown()) {
			throw new RejectedExecutionException("Connection pool is shut down");
		}
		return connPool.lease(tgtHost, null, new ConnLeaseFutureCallback(connPool, wsIoTask));
	}
	//
	private final class ConnLeaseFutureCallback
	implements FutureCallback<BasicNIOPoolEntry> {
		//
		private final HttpConnPool<HttpHost, BasicNIOPoolEntry> connPool;
		private final HttpDataIoTask<T> wsIoTask;
		//
		public ConnLeaseFutureCallback(
			final HttpConnPool<HttpHost, BasicNIOPoolEntry> connPool,
			final HttpDataIoTask<T> wsIoTask
		) {
			this.connPool = connPool;
			this.wsIoTask = wsIoTask;
		}
		//
		@Override
		public final void completed(final BasicNIOPoolEntry connPoolEntry) {
			incrementBusyThreadCount();
			client.execute(
				wsIoTask, wsIoTask, connPoolEntry, connPool, wsIoTask, ioTaskFutureCallback
			);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "I/O task #{} has been submitted for execution",
					wsIoTask.hashCode()
				);
			}
		}
		//
		@Override
		public final void failed(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Connection lease failed");
		}
		//
		@Override
		public final void cancelled() {
			LOG.debug(Markers.MSG, "Connection lease cancelled");
		}
	}
	//
	private final FutureCallback<HttpDataIoTask<T>> ioTaskFutureCallback = new FutureCallback<HttpDataIoTask<T>>() {
		//
		@Override
		public final void completed(final HttpDataIoTask<T> ioTask) {
			try {
				ioTaskCompleted(ioTask);
			} catch(final RemoteException ignore) {
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
	@Override @SuppressWarnings("unchecked")
	public <A extends IoTask<T>> int submitTasks(final List<A> ioTasks, int from, int to)
	throws RejectedExecutionException {
		int n = 0;
		if(isPipeliningEnabled) {
			if(ioTasks.size() > 0) {
				final List<HttpDataIoTask<T>> wsIoTasks = (List<HttpDataIoTask<T>>) ioTasks;
				final HttpDataIoTask<T> anyTask = wsIoTasks.get(0);
				final HttpHost tgtHost = anyTask.getTarget();
				final HttpConnPool<HttpHost, BasicNIOPoolEntry> connPool = connPoolMap.get(tgtHost);
				try {
					connPool.lease(
						tgtHost, null,
						new ConnLeaseFutureBatchCallback(connPool, wsIoTasks, from, to)
					);
					n = to - from;
				} catch(final Exception e) {
					throw new RejectedExecutionException(e);
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
	private final class ConnLeaseFutureBatchCallback
	implements FutureCallback<BasicNIOPoolEntry> {
		//
		private final HttpConnPool<HttpHost, BasicNIOPoolEntry> connPool;
		private final List<HttpDataIoTask<T>> wsIoTasks;
		private final int from;
		private final int to;
		//
		public ConnLeaseFutureBatchCallback(
			final HttpConnPool<HttpHost, BasicNIOPoolEntry> connPool,
			final List<HttpDataIoTask<T>> wsIoTasks, final int from, final int to
		) {
			this.connPool = connPool;
			this.wsIoTasks = wsIoTasks;
			this.from = from;
			this.to = to;
		}
		//
		@Override @SuppressWarnings("unchecked")
		public final void completed(final BasicNIOPoolEntry connPoolEntry) {
			incrementBusyThreadCount();
			final List wsIoTasks_;
			if(from > 0 || to < wsIoTasks.size()) {
				wsIoTasks_ = wsIoTasks.subList(from, to);
			} else {
				wsIoTasks_ = wsIoTasks;
			}
			client.executePipelined(
				wsIoTasks_, wsIoTasks_, connPoolEntry, connPool, HttpCoreContext.create(),
				new BatchFutureCallback(wsIoTasks_)
			);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "{} I/O tasks have been submitted for execution", from - to
				);
			}
		}
		//
		@Override
		public final void failed(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Connection lease failed");
		}
		//
		@Override
		public final void cancelled() {
			LOG.debug(Markers.MSG, "Connection lease cancelled");
		}
	}
	//
	private final class BatchFutureCallback
	implements FutureCallback<List<HttpDataIoTask<T>>> {
		//
		private final List<HttpDataIoTask<T>> tasks;
		//
		private BatchFutureCallback(final List<HttpDataIoTask<T>> tasks) {
			this.tasks = tasks;
		}
		//
		@Override
		public final void completed(final List<HttpDataIoTask<T>> result) {
			try {
				ioTaskCompletedBatch(result, 0, result.size());
			} catch(final RemoteException e) {
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
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Balancing based on the connection pool stats
	////////////////////////////////////////////////////////////////////////////////////////////////
	/*private volatile Set<HttpHost> routes = null;
	private final static ThreadLocal<Map<HttpHost, String>>
		THREAD_CACHED_REVERSE_NODE_MAP = new ThreadLocal<>();
	@Override
	protected final String getNextNode() {
		HttpHost nodeHost = null;
		// connPool.getRoutes() is quite expensive, so reuse the routes set
		if(routes == null || routes.size() < storageNodeCount) {
			routes = connPool.getRoutes();
		} else {
			// select the route having the max count of the free connections in the pool
			// TODO think how to not to invoke connPool.getStats(HttpHost route)
			int maxConnCount = -1, nextConnCount;
			for(final HttpHost nextRoute : routes) {
				nextConnCount = connPool.getStats(nextRoute).getAvailable();
				if(nextConnCount > maxConnCount) {
					maxConnCount = nextConnCount;
					nodeHost = nextRoute;
				}
			}
		}
		//
		String nodeAddr;
		if(nodeHost == null) { // fallback
			nodeAddr = super.getNextNode();
		} else {
			Map<HttpHost, String> cachedReverseNodeMap = THREAD_CACHED_REVERSE_NODE_MAP.get();
			if(cachedReverseNodeMap == null) {
				cachedReverseNodeMap = new HashMap<>();
				THREAD_CACHED_REVERSE_NODE_MAP.set(cachedReverseNodeMap);
			}
			nodeAddr = cachedReverseNodeMap.get(nodeHost);
			if(nodeAddr == null) {
				nodeAddr = nodeHost.toHostString();
				cachedReverseNodeMap.put(nodeHost, nodeAddr);
			}
		}
		return nodeAddr;
	}*/
}
