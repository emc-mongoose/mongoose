package com.emc.mongoose.client.impl.load.executor;
//
import com.emc.mongoose.client.api.load.executor.HttpDataLoadClient;
//
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtil;
//
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.executor.HttpDataLoadExecutor;
import com.emc.mongoose.core.api.load.executor.MixedLoadExecutor;
//
import com.emc.mongoose.core.api.load.model.metrics.IOStats;
//
import com.emc.mongoose.server.api.load.executor.HttpDataLoadSvc;
import com.emc.mongoose.server.api.load.executor.MixedHttpDataLoadSvc;
//
import org.apache.commons.lang.text.StrBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
	private final Map<LoadType, HttpDataLoadClient<T, HttpDataLoadSvc<T>>>
		loadClientMap = new HashMap<>();
	private final Map<LoadType, Integer>
		loadTypeWeightMap;
	//
	public BasicMixedHttpDataLoadClient(
		final AppConfig appConfig, final HttpRequestConfig<T, ? extends Container<T>> reqConfig,
		final String[] addrs, final int threadCount, final long maxCount, final float rateLimit,
		final Map<String, W> remoteLoadMap, final Map<LoadType, Input<T>> itemInputMap,
		final Map<LoadType, Integer> loadTypeWeightMap
	) throws RemoteException {
		//
		super(
			appConfig, reqConfig, addrs, threadCount, null, maxCount, rateLimit, remoteLoadMap,
			remoteLoadMap.values().iterator().next().getInstanceNum()
		);
		this.loadTypeWeightMap = loadTypeWeightMap;
		//
		Map<String, HttpDataLoadSvc<T>> nextRemoteLoadMap;
		for(final LoadType nextLoadType : itemInputMap.keySet()) {
			final HttpRequestConfig<T, ? extends Container<T>> reqConfigCopy;
			try {
				reqConfigCopy = (HttpRequestConfig<T, ? extends Container<T>>) reqConfig
					.clone().setLoadType(nextLoadType);
			} catch(final CloneNotSupportedException e) {
				throw new IllegalStateException(e);
			}
			//
			nextRemoteLoadMap = new HashMap<>();
			W nextMixedLoadSvc;
			String nextWrappedLoadSvcName;
			HttpDataLoadSvc<T> nextWrappedLoadSvc;
			for(final String svcAddr : remoteLoadMap.keySet()) {
				nextMixedLoadSvc = remoteLoadMap.get(svcAddr);
				nextWrappedLoadSvcName = nextMixedLoadSvc.getWrappedLoadSvcNameFor(nextLoadType);
				nextWrappedLoadSvc = (HttpDataLoadSvc<T>) ServiceUtil
					.getRemoteSvc("//" + svcAddr + "/" + nextWrappedLoadSvcName);
				nextRemoteLoadMap.put(svcAddr, nextWrappedLoadSvc);
			}
			//
			final BasicHttpDataLoadClient<T, HttpDataLoadSvc<T>>
				nextLoadClient = new BasicHttpDataLoadClient<>(
					appConfig, reqConfigCopy, addrs, threadCount, itemInputMap.get(nextLoadType),
					maxCount, rateLimit, nextRemoteLoadMap
				);
			loadClientMap.put(nextLoadType, nextLoadClient);
		}
	}
	//
	@Override
	public void logMetrics(final Marker logMarker) {
		final StrBuilder strb = new StrBuilder(Markers.PERF_SUM.equals(logMarker) ? "Summary:" : "")
			.appendNewLine()
			.appendFixedWidthPadLeft("Weight | ", 9, ' ')
			.appendFixedWidthPadLeft("Load type | ", 12, ' ')
			.appendFixedWidthPadLeft("Done | ", 14, ' ')
			.appendFixedWidthPadLeft("Failed | ", 9, ' ')
			.appendFixedWidthPadLeft("Duration [us] | ", 35, ' ')
			.appendFixedWidthPadLeft("Latency [us] | ", 35, ' ')
			.appendFixedWidthPadLeft("TP [op/s] | ", 25, ' ')
			.appendFixedWidthPadLeft("BW [MB/s]", 22, ' ')
			.appendNewLine()
			.appendPadding(160, '-')
			.appendNewLine();
		HttpDataLoadExecutor nextLoadJob;
		int nextLoadWeight;
		IOStats.Snapshot nextLoadStats;
		for(final LoadType nextLoadType : loadTypeWeightMap.keySet()) {
			nextLoadWeight = loadTypeWeightMap.get(nextLoadType);
			nextLoadJob = loadClientMap.get(nextLoadType);
			try {
				nextLoadStats = nextLoadJob.getStatsSnapshot();
			} catch(final RemoteException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to get the remote stats snapshot");
				continue;
			}
			strb
				.appendFixedWidthPadLeft(nextLoadWeight + " % | ", 9, ' ')
				.appendFixedWidthPadLeft(nextLoadType.name() + " | ", 12, ' ')
				.appendFixedWidthPadLeft(nextLoadStats.getSuccCount() + " | ", 14, ' ')
				.appendFixedWidthPadLeft(nextLoadStats.getFailCount() + " | ", 9, ' ')
				.appendFixedWidthPadLeft(
					(
						Markers.PERF_SUM.equals(logMarker) ?
							nextLoadStats.toDurSummaryString() : nextLoadStats.toDurString()
					) + " | ", 35, ' '
				)
				.appendFixedWidthPadLeft(
					(
						Markers.PERF_SUM.equals(logMarker) ?
							nextLoadStats.toLatSummaryString() : nextLoadStats.toLatString()
					) + " | ", 35, ' '
				)
				.appendFixedWidthPadLeft(nextLoadStats.toSuccRatesString() + " | ", 25, ' ')
				.appendFixedWidthPadLeft(nextLoadStats.toByteRatesString(), 22, ' ')
				.appendNewLine();
		}
		strb
			.appendPadding(160, '-').appendNewLine()
			.appendFixedWidthPadLeft("100 % | ", 9, ' ')
			.appendFixedWidthPadLeft("TOTAL | ", 12, ' ')
			.appendFixedWidthPadLeft(lastStats.getSuccCount() + " | ", 14, ' ')
			.appendFixedWidthPadLeft(lastStats.getFailCount() + " | ", 9, ' ')
			.appendFixedWidthPadLeft(
				(
					Markers.PERF_SUM.equals(logMarker) ?
						lastStats.toDurSummaryString() : lastStats.toDurString()
				) + " | ", 35, ' '
			)
			.appendFixedWidthPadLeft(
				(
					Markers.PERF_SUM.equals(logMarker) ?
						lastStats.toLatSummaryString() : lastStats.toLatString()
				) + " | ", 35, ' '
			)
			.appendFixedWidthPadLeft(lastStats.toSuccRatesString() + " | ", 25, ' ')
			.appendFixedWidthPadLeft(lastStats.toByteRatesString(), 22, ' ');
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
