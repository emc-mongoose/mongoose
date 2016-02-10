package com.emc.mongoose.core.impl.load.executor;
// mongoose-common.jar
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.io.IOWorker;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.http.conn.pool.HttpConnPool;
import com.emc.mongoose.common.net.http.conn.pool.FixedRouteSequencingConnPool;
import com.emc.mongoose.common.net.http.request.SharedHeadersAdder;
import com.emc.mongoose.common.net.http.request.HostHeaderSetter;
import com.emc.mongoose.common.log.LogUtil;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.task.HttpDataIOTask;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
import com.emc.mongoose.core.api.load.executor.HttpDataLoadExecutor;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.io.task.BasicHttpDataIOTask;
import com.emc.mongoose.core.impl.load.tasks.HttpClientRunTask;
//
import org.apache.http.ExceptionLogger;
import org.apache.http.HttpHost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.pool.BasicNIOPoolEntry;
import org.apache.http.message.HeaderGroup;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestUserAgent;
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
import org.apache.logging.log4j.Marker;
//
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by kurila on 02.12.14.
 */
public class BasicHttpDataLoadExecutor<T extends HttpDataItem>
extends MutableDataLoadExecutorBase<T>
implements HttpDataLoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final HttpProcessor httpProcessor;
	private final HttpAsyncRequester client;
	private final ConnectingIOReactor ioReactor;
	private final Map<HttpHost, HttpConnPool<HttpHost, BasicNIOPoolEntry>> connPoolMap;
	private final HttpRequestConfig<T, Container<T>> httpReqConfigCopy;
	private final boolean isPipeliningEnabled;
	//
	private final AtomicLong
		connLeaseCount = new AtomicLong(0),
		connReleaseCount = new AtomicLong(0);
	//
	@SuppressWarnings("unchecked")
	public BasicHttpDataLoadExecutor(
		final AppConfig appConfig, final HttpRequestConfig<T, ? extends Container<T>> reqConfig,
		final String[] addrs, final int threadCount, final ItemSrc<T> itemSrc, final long maxCount,
		final float rateLimit
	) {
		super(appConfig, reqConfig, addrs, threadCount, itemSrc, maxCount, rateLimit);
		httpReqConfigCopy = (HttpRequestConfig<T, Container<T>>) ioConfigCopy;
		isPipeliningEnabled = httpReqConfigCopy.getPipelining();
		//
		final HeaderGroup sharedHeaders = httpReqConfigCopy.getSharedHeaders();
		final String userAgent = appConfig.getRunName() + "/" + appConfig.getRunVersion();
		//
		httpProcessor = HttpProcessorBuilder
			.create()
			.add(new SharedHeadersAdder(sharedHeaders))
			.add(new HostHeaderSetter())
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
		final AppConfig thrLocalConfig = BasicConfig.THREAD_CONTEXT.get();
		final int buffSize = httpReqConfigCopy.getBuffSize();
		final long timeOutMs = TimeUnit.SECONDS.toMillis(appConfig.getLoadLimitTime());
		final IOReactorConfig.Builder ioReactorConfigBuilder = IOReactorConfig
			.custom()
			.setIoThreadCount(threadCount)
			.setBacklogSize(thrLocalConfig.getNetworkSocketBindBacklogSize())
			.setInterestOpQueued(thrLocalConfig.getNetworkSocketInterestOpQueued())
			.setSelectInterval(thrLocalConfig.getNetworkSocketSelectInterval())
			.setShutdownGracePeriod(thrLocalConfig.getNetworkSocketTimeoutMilliSec())
			.setSoKeepAlive(thrLocalConfig.getNetworkSocketKeepAlive())
			.setSoLinger(thrLocalConfig.getNetworkSocketLinger())
			.setSoReuseAddress(thrLocalConfig.getNetworkSocketReuseAddr())
			.setSoTimeout(thrLocalConfig.getNetworkSocketTimeoutMilliSec())
			.setTcpNoDelay(thrLocalConfig.getNetworkSocketTcpNoDelay())
			.setRcvBufSize(IOTask.Type.READ.equals(loadType) ? buffSize : Constants.BUFF_SIZE_LO)
			.setSndBufSize(IOTask.Type.READ.equals(loadType) ? Constants.BUFF_SIZE_LO : buffSize)
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
		final IOWorker.Factory ioWorkerFactory = new IOWorker.Factory(getName());
		try {
			ioReactor = new DefaultConnectingIOReactor(
				ioReactorConfigBuilder.build(), ioWorkerFactory
			);
		} catch(final IOReactorException e) {
			throw new IllegalStateException("Failed to build the I/O reactor", e);
		}
		//
		final NIOConnFactory<HttpHost, NHttpClientConnection>
			connFactory = new BasicNIOConnFactory(
				null, null, null, null,
				DirectByteBufferAllocator.INSTANCE, connConfig
			);
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
	public final void logMetrics(final Marker logMarker) {
		super.logMetrics(logMarker);
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, "Connections: leased={}, released={}",
				connLeaseCount.get(), connReleaseCount.get()
			);
		}
	}
	//
	@Override
	protected HttpDataIOTask<T> getIOTask(final T item, final String nodeAddr) {
		return new BasicHttpDataIOTask<>(item, nodeAddr, httpReqConfigCopy);
	}
	//
	@Override
	protected void interruptActually() {
		try {
			super.interruptActually();
		} finally {
			for(final HttpConnPool<HttpHost, BasicNIOPoolEntry> nextConnPool : connPoolMap.values()) {
				nextConnPool.closeExpired();
				LOG.debug(
					Markers.MSG, "{}: closed expired (if any) connections in the pool", getName()
				);
				try {
					nextConnPool.closeIdle(1, TimeUnit.MILLISECONDS);
					LOG.debug(
						Markers.MSG, "{}: closed idle connections (if any) in the pool", getName()
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
	protected <A extends IOTask<T>> Future<A> submitTaskActually(final A ioTask)
	throws RejectedExecutionException {
		//
		final HttpDataIOTask<T> wsTask = (HttpDataIOTask<T>) ioTask;
		final HttpConnPool<HttpHost, BasicNIOPoolEntry>
			connPool = connPoolMap.get(wsTask.getTarget());
		if(connPool.isShutdown()) {
			throw new RejectedExecutionException("Connection pool is shut down");
		}
		//
		final Future<HttpDataIOTask<T>> futureResult;
		try {
			futureResult = client.execute(wsTask, wsTask, connPool, wsTask, futureCallback);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "I/O task #{} has been submitted for execution", wsTask.hashCode()
				);
			}
		} catch(final Exception e) {
			throw new RejectedExecutionException(e);
		}
		return (Future<A>) futureResult;
	}
	//
	private final FutureCallback<HttpDataIOTask<T>> futureCallback = new FutureCallback<HttpDataIOTask<T>>() {
		@Override
		public final void completed(final HttpDataIOTask<T> ioTask) {
			ioTaskCompleted(ioTask);
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
	public final int submitTasks(final List<? extends IOTask<T>> ioTasks, int from, int to)
	throws RejectedExecutionException {
		int n = 0;
		if(isPipeliningEnabled) {
			if(ioTasks.size() > 0) {
				final List<HttpDataIOTask<T>> wsIOTasks = (List<HttpDataIOTask<T>>) ioTasks;
				final HttpDataIOTask<T> anyTask = wsIOTasks.get(0);
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
				if(null != submitReq(ioTasks.get(i))) {
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
	implements FutureCallback<List<HttpDataIOTask<T>>> {
		//
		private final List<HttpDataIOTask<T>> tasks;
		//
		private BatchFutureCallback(final List<HttpDataIOTask<T>> tasks) {
			this.tasks = tasks;
		}
		//
		@Override
		public final void completed(final List<HttpDataIOTask<T>> result) {
			ioTaskCompletedBatch(result, 0, result.size());
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
