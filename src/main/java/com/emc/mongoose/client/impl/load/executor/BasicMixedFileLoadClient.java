package com.emc.mongoose.client.impl.load.executor;
//
import com.emc.mongoose.client.api.load.executor.FileLoadClient;
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.core.api.io.conf.FileIoConfig;
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.load.executor.FileLoadExecutor;
import com.emc.mongoose.core.api.load.executor.MixedLoadExecutor;
import com.emc.mongoose.core.api.load.model.metrics.IOStats;
import com.emc.mongoose.server.api.load.executor.FileLoadSvc;
import com.emc.mongoose.server.api.load.executor.MixedFileLoadSvc;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 05.04.16.
 */
public class BasicMixedFileLoadClient<F extends FileItem, W extends MixedFileLoadSvc<F>>
extends BasicFileLoadClient<F, W>
implements FileLoadClient<F, W>, MixedLoadExecutor<F> {
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Map<LoadType, FileLoadClient<F, FileLoadSvc<F>>>
		loadClientMap = new HashMap<>();
	private final Map<LoadType, Integer>
		loadTypeWeightMap;
	//
	public BasicMixedFileLoadClient(
		final AppConfig appConfig, final FileIoConfig<F, ? extends Directory<F>> reqConfig,
		final int threadCount, final long countLimit, final long sizeLimit, final float rateLimit,
		final Map<String, W> remoteLoadMap, final Map<LoadType, Input<F>> itemInputMap,
		final Map<LoadType, Integer> loadTypeWeightMap
	) throws RemoteException {
		//
		super(
			appConfig, reqConfig, threadCount, null, countLimit, sizeLimit, rateLimit,
			remoteLoadMap, remoteLoadMap.values().iterator().next().getInstanceNum()
		);
		this.loadTypeWeightMap = loadTypeWeightMap;
		//
		Map<String, FileLoadSvc<F>> nextRemoteLoadMap;
		for(final LoadType nextLoadType : itemInputMap.keySet()) {
			final FileIoConfig<F, ? extends Directory<F>> reqConfigCopy;
			try {
				reqConfigCopy = (FileIoConfig<F, ? extends Directory<F>>) reqConfig
					.clone().setLoadType(nextLoadType);
			} catch(final CloneNotSupportedException e) {
				throw new IllegalStateException(e);
			}
			//
			nextRemoteLoadMap = new HashMap<>();
			W nextMixedLoadSvc;
			String nextWrappedLoadSvcName;
			FileLoadSvc<F> nextWrappedLoadSvc;
			for(final String svcAddr : remoteLoadMap.keySet()) {
				nextMixedLoadSvc = remoteLoadMap.get(svcAddr);
				nextWrappedLoadSvcName = nextMixedLoadSvc.getWrappedLoadSvcNameFor(nextLoadType);
				nextWrappedLoadSvc = (FileLoadSvc<F>) ServiceUtil
					.getRemoteSvc("//" + svcAddr + "/" + nextWrappedLoadSvcName);
				nextRemoteLoadMap.put(svcAddr, nextWrappedLoadSvc);
			}
			//
			final BasicFileLoadClient<F, FileLoadSvc<F>>
				nextLoadClient = new BasicFileLoadClient<>(
				appConfig, reqConfigCopy, threadCount, itemInputMap.get(nextLoadType),
				countLimit, sizeLimit, rateLimit, nextRemoteLoadMap
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
		FileLoadExecutor<F> nextLoadJob;
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
		for(final FileLoadClient<F, FileLoadSvc<F>> nextLoadClient : loadClientMap.values()) {
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
		for(final FileLoadClient<F, FileLoadSvc<F>> nextLoadClient : loadClientMap.values()) {
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
		for(final FileLoadClient<F, FileLoadSvc<F>> nextLoadClient : loadClientMap.values()) {
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
		for(final FileLoadClient<F, FileLoadSvc<F>> nextLoadClient : loadClientMap.values()) {
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
						BasicMixedFileLoadClient.super.await(timeOut, timeUnit);
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
		for(final FileLoadClient<F, FileLoadSvc<F>> nextLoadClient : loadClientMap.values()) {
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
