package com.emc.mongoose.client.impl.load.executor;
//
import com.emc.mongoose.client.api.load.executor.HttpDataLoadClient;
//
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.executor.MixedLoadExecutor;
import com.emc.mongoose.core.api.load.model.metrics.IOStats;
//
import com.emc.mongoose.core.impl.load.model.WeightBarrier;
//
import com.emc.mongoose.server.api.load.executor.HttpDataLoadSvc;
import com.emc.mongoose.server.api.load.executor.MixedHttpDataLoadSvc;
//
import org.apache.commons.lang.text.StrBuilder;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 30.03.16.
 */
public class BasicMixedHttpDataLoadClient<T extends HttpDataItem, W extends MixedHttpDataLoadSvc<T>>
extends BasicHttpDataLoadClient<T, W>
implements HttpDataLoadClient<T, W>, MixedLoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final WeightBarrier<LoadType, IOTask<T>> barrier;
	private final Map<LoadType, Integer> loadTypeWeights;
	private final Map<LoadType, HttpRequestConfig<T, ? extends Container<T>>>
		reqConfigMap = new HashMap<>();
	private final Map<LoadType, HttpDataLoadClient<T, HttpDataLoadSvc<T>>>
		loadClientMap = new HashMap<>();
	//
	public BasicMixedHttpDataLoadClient(
		final AppConfig appConfig, final HttpRequestConfig<T, ? extends Container<T>> reqConfig,
		final String[] addrs, final int threadCount, final long maxCount, final float rateLimit,
		final Map<String, W> remoteLoadMap, final Map<LoadType, Integer> loadTypeWeightMap,
		final Map<LoadType, ItemSrc<T>> itemSrcMap
	) throws RemoteException {
		//
		super(appConfig, reqConfig, addrs, threadCount, null, maxCount, rateLimit, remoteLoadMap);
		//
		this.loadTypeWeights = loadTypeWeightMap;
		this.barrier = new WeightBarrier<>(loadTypeWeights);
		//
		W nextMixedLoadSvc;
		Map<LoadType, String> wrappedLoadSvcNames;
		Map<String, HttpDataLoadSvc<T>> remoteMixedLoadMap;
		//
		for(final LoadType loadType : loadTypeWeights.keySet()) {
			final HttpRequestConfig<T, ? extends Container<T>> reqConfigCopy;
			try {
				reqConfigCopy = (HttpRequestConfig<T, ? extends Container<T>>) reqConfig
					.clone().setLoadType(loadType);
			} catch(final CloneNotSupportedException e) {
				throw new IllegalStateException(e);
			}
			reqConfigMap.put(loadType, reqConfigCopy);
			//
			remoteMixedLoadMap = new HashMap<>();
			for(final String loadServerAddr : remoteLoadMap.keySet()) {
				nextMixedLoadSvc = remoteLoadMap.get(loadServerAddr);
				wrappedLoadSvcNames = nextMixedLoadSvc.getWrappedLoadSvcNames();
				remoteMixedLoadMap.put(
					loadServerAddr,
					(HttpDataLoadSvc<T>) ServiceUtil.getRemoteSvc(
						ServiceUtil.getSvcUrl(wrappedLoadSvcNames.get(loadType))
					)
				);
			}
			//
			final BasicHttpDataLoadClient<T, HttpDataLoadSvc<T>>
				nextLoadClient = new BasicHttpDataLoadClient<T, HttpDataLoadSvc<T>>(
					appConfig, reqConfigCopy, addrs, threadCount, itemSrcMap.get(loadType),
					maxCount, rateLimit, remoteMixedLoadMap
				) {
					@Override
					public final <A extends IOTask<T>> Future<A> submitTask(final A ioTask)
					throws RemoteException, RejectedExecutionException {
						return BasicMixedHttpDataLoadClient.this.submitTask(ioTask);
					}
					//
					@Override
					public final <A extends IOTask<T>> int submitTasks(
						final List<A> ioTasks, int from, int to
					) throws RemoteException, RejectedExecutionException {
						return BasicMixedHttpDataLoadClient.this.submitTasks(ioTasks, from, to);
					}
				};
			loadClientMap.put(loadType, nextLoadClient);
		}
	}
	//
	@Override
	public final <A extends IOTask<T>> Future<A> submitTask(final A ioTask)
	throws RemoteException, RejectedExecutionException {
		try {
			if(barrier.getApprovalFor(ioTask)) {
				return super.submitTask(ioTask);
			} else {
				throw new RejectedExecutionException(
					"Barrier rejected the task #" + ioTask.hashCode()
				);
			}
		} catch(final InterruptedException e) {
			throw new RejectedExecutionException(e);
		}
	}
	//
	@Override
	public final <A extends IOTask<T>> int submitTasks(final List<A> ioTasks, int from, int to)
	throws RemoteException, RejectedExecutionException {
		try {
			if(barrier.getBatchApprovalFor((List<IOTask<T>>) ioTasks, from, to)) {
				return super.submitTasks(ioTasks, from, to);
			} else {
				throw new RejectedExecutionException(
					"Barrier rejected " + (to - from) + " tasks"
				);
			}
		} catch(final InterruptedException e) {
			throw new RejectedExecutionException(e);
		}
	}
	//
	@Override
	public void logMetrics(final Marker logMarker) {
		final StrBuilder strb = new StrBuilder()
			.appendNewLine()
			.appendPadding(100, '-')
			.appendNewLine();
		HttpDataLoadClient<T, HttpDataLoadSvc<T>> nextLoadJobClient;
		int nextLoadWeight;
		IOStats.Snapshot nextLoadStats = null;
		for(final LoadType nextLoadType : loadClientMap.keySet()) {
			nextLoadWeight = loadTypeWeights.get(nextLoadType);
			nextLoadJobClient = loadClientMap.get(nextLoadType);
			try {
				nextLoadStats = nextLoadJobClient.getStatsSnapshot();
			} catch(final RemoteException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to get the remote stats snapshot");
			}
			strb
				.appendFixedWidthPadLeft(nextLoadWeight + " % ", 6, ' ')
				.appendFixedWidthPadLeft(nextLoadType.name() + " ", 10, ' ')
				.append(
					nextLoadStats == null ?
						null :
						Markers.PERF_SUM.equals(logMarker) ?
							nextLoadStats.toSummaryString() : nextLoadStats.toString()
				)
				.appendNewLine();

		}
		strb
			.appendPadding(100, '-').appendNewLine()
			.appendFixedWidthPadLeft("100 %     TOTAL ", 16, ' ')
			.append(
				Markers.PERF_SUM.equals(logMarker) ?
					lastStats.toSummaryString() : lastStats.toString()
			)
			.appendNewLine()
			.appendPadding(100, '-');
		LOG.info(Markers.MSG, strb.toString());
	}
	//
	@Override
	protected void startActually() {
		for(final HttpDataLoadClient<T, HttpDataLoadSvc<T>> nextLoadClient : loadClientMap.values()) {
			try {
				nextLoadClient.start();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to start the load job \"{}\"", nextLoadClient
				);
			}
		}
		super.startActually();
	}
	//
	@Override
	protected void interruptActually() {
		for(final HttpDataLoadClient<T, HttpDataLoadSvc<T>> nextLoadClient : loadClientMap.values()) {
			try {
				nextLoadClient.interrupt();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to interrupt the load job \"{}\"", nextLoadClient
				);
			}
		}
		super.interruptActually();
	}
	//
	@Override
	protected void shutdownActually() {
		for(final HttpDataLoadClient<T, HttpDataLoadSvc<T>> nextLoadClient : loadClientMap.values()) {
			try {
				nextLoadClient.shutdown();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to shutdown the load job \"{}\"", nextLoadClient
				);
			}
		}
		super.shutdownActually();
	}
	//
	@Override
	public void await(final long timeOut, final TimeUnit timeUnit)
	throws InterruptedException, RemoteException {
		final ExecutorService awaitExecutor = Executors.newFixedThreadPool(
			loadClientMap.size() + 1, new GroupThreadFactory("await<" + getName() + ">", true)
		);
		for(final HttpDataLoadClient<T, HttpDataLoadSvc<T>> nextLoadClient : loadClientMap.values()) {
			awaitExecutor.submit(
				new Runnable() {
					@Override
					public final void run() {
						try {
							nextLoadClient.await(timeOut, timeUnit);
						} catch(final RemoteException e) {
							LogUtil.exception(
								LOG, Level.ERROR, e, "Failed to await the load job \"{}\"",
								nextLoadClient
							);
						} catch(final InterruptedException e) {
							LOG.debug(Markers.MSG, "{}: await call interrupted", nextLoadClient);
						}
					}
				}
			);
		}
		awaitExecutor.submit(
			new Runnable() {
				@Override
				public final void run() {
					try {
						BasicMixedHttpDataLoadClient.super.await(timeOut, timeUnit);
					} catch(final InterruptedException e) {
						LOG.debug(Markers.MSG, "{}: await call interrupted", getName());
					} catch(final RemoteException e) {
						LogUtil.exception(LOG, Level.ERROR, e, "Remote await method call failure");
					}
				}
			}
		);
		awaitExecutor.shutdown();
		if(awaitExecutor.awaitTermination(timeOut, timeUnit)) {
			LOG.debug(Markers.MSG, "{}: await completed before the timeout", getName());
		} else {
			LOG.debug(Markers.MSG,
				"{}: await timeout, {} await tasks dropped",
				getName(), awaitExecutor.shutdownNow().size()
			);
		}
	}
	//
	@Override
	protected void closeActually()
	throws IOException {
		for(final HttpDataLoadClient<T, HttpDataLoadSvc<T>> nextLoadClient : loadClientMap.values()) {
			try {
				nextLoadClient.close();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to close the load job \"{}\"", nextLoadClient
				);
			}
		}
		super.closeActually();
	}
}
