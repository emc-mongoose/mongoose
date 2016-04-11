package com.emc.mongoose.server.impl.load.executor;
//
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.DataRangesConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
import com.emc.mongoose.core.api.io.task.IoTask;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.barrier.Barrier;
import com.emc.mongoose.core.api.load.executor.HttpDataLoadExecutor;
import com.emc.mongoose.core.api.load.metrics.IOStats;
//
import com.emc.mongoose.core.impl.load.barrier.WeightBarrier;
//
import com.emc.mongoose.server.api.load.executor.HttpDataLoadSvc;
//
import com.emc.mongoose.server.api.load.executor.MixedHttpDataLoadSvc;
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
public class BasicMixedHttpDataLoadSvc<T extends HttpDataItem>
extends BasicHttpDataLoadSvc<T>
implements MixedHttpDataLoadSvc<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Barrier<LoadType> barrier;
	private final Map<LoadType, Integer> loadTypeWeights;
	private final Map<LoadType, HttpRequestConfig<T, ? extends Container<T>>>
		reqConfigMap = new HashMap<>();
	protected final Map<LoadType, HttpDataLoadSvc<T>>
		loadSvcMap = new HashMap<>();

	//
	public BasicMixedHttpDataLoadSvc(
		final AppConfig appConfig, final HttpRequestConfig<T, ? extends Container<T>> reqConfig,
		final String[] addrs, final int threadCount, final long maxCount, final float rateLimit,
		final SizeInBytes sizeConfig, final DataRangesConfig rangesConfig,
		final Map<LoadType, Integer> loadTypeWeightMap,
		final Map<LoadType, Input<T>> itemInputMap
	) {
		super(
			appConfig, reqConfig, addrs, threadCount, null, maxCount, rateLimit, sizeConfig,
			rangesConfig
		);
		//
		this.loadTypeWeights = loadTypeWeightMap;
		this.barrier = new WeightBarrier<>(loadTypeWeights, isInterrupted);
		for(final LoadType nextLoadType : loadTypeWeights.keySet()) {
			final HttpRequestConfig<T, ? extends Container<T>> reqConfigCopy;
			try {
				reqConfigCopy = (HttpRequestConfig<T, ? extends Container<T>>) reqConfig
					.clone().setLoadType(nextLoadType);
			} catch(final CloneNotSupportedException e) {
				throw new IllegalStateException(e);
			}
			reqConfigMap.put(nextLoadType, reqConfigCopy);
			final BasicHttpDataLoadSvc<T> nextLoadSvc = new BasicHttpDataLoadSvc<T>(
				appConfig, reqConfigCopy, addrs, threadCount, itemInputMap.get(nextLoadType),
				maxCount, rateLimit, sizeConfig, rangesConfig,
				httpProcessor, client, ioReactor, connPoolMap
			) {
				@Override
				public final <A extends IoTask<T>> Future<A> submitTask(final A ioTask)
				throws RejectedExecutionException {
					try {
						if(barrier.getApprovalFor(nextLoadType)) {
							return BasicMixedHttpDataLoadSvc.this.submitTask(ioTask);
						} else {
							throw new RejectedExecutionException(
								"Barrier rejected the item for {} operation" + nextLoadType
							);
						}
					} catch(final InterruptedException e) {
						throw new RejectedExecutionException(e);
					}
				}
				//
				@Override
				public final <A extends IoTask<T>> int submitTasks(
					final List<A> ioTasks, int from, int to
				) throws RejectedExecutionException {
					try {
						if(barrier.getApprovalsFor(nextLoadType, to - from)) {
							return BasicMixedHttpDataLoadSvc.this.submitTasks(
								ioTasks, from, to
							);
						} else {
							throw new RejectedExecutionException(
								"Barrier rejected " + (to - from) + " tasks"
							);
						}
					} catch(final InterruptedException e) {
						throw new RejectedExecutionException(e);
					}
				}
			};
			try {
				ServiceUtil.create(nextLoadSvc);
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to export the load service \"{}\"",
					nextLoadSvc.getName()
				);
			}
			loadSvcMap.put(nextLoadType, nextLoadSvc);
		}
	}
	//
	@Override
	public final void ioTaskCompleted(final IoTask<T> ioTask)
	throws RemoteException {
		loadSvcMap.get(ioTask.getLoadType())
			.ioTaskCompleted(ioTask);
		super.ioTaskCompleted(ioTask);
	}
	//
	@Override
	public final int ioTaskCompletedBatch(
		final List<? extends IoTask<T>> ioTasks, final int from, final int to
	) throws RemoteException {
		if(ioTasks != null && ioTasks.size() > 0) {
			loadSvcMap.get(ioTasks.get(0).getLoadType()).ioTaskCompletedBatch(ioTasks, from, to);
		}
		return super.ioTaskCompletedBatch(ioTasks, from, to);
	}
	//
	@Override
	public void logMetrics(final Marker logMarker) {
		final StrBuilder strb = new StrBuilder()
			.appendNewLine()
			.appendPadding(100, '-')
			.appendNewLine();
		HttpDataLoadExecutor nextLoadJob;
		int nextLoadWeight;
		IOStats.Snapshot nextLoadStats = null;
		for(final LoadType nextLoadType : loadSvcMap.keySet()) {
			nextLoadWeight = loadTypeWeights.get(nextLoadType);
			nextLoadJob = loadSvcMap.get(nextLoadType);
			try {
				nextLoadStats = nextLoadJob.getStatsSnapshot();
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
	/*
	@Override
	protected void startActually() {
		for(final HttpDataLoadSvc<T> nextLoadSvc : loadSvcMap.values()) {
			try {
				nextLoadSvc.start();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to start the load job \"{}\"", nextLoadSvc
				);
			}
		}
		super.startActually();
	}*/
	//
	@Override
	protected void interruptActually() {
		for(final HttpDataLoadSvc<T> nextLoadSvc : loadSvcMap.values()) {
			try {
				nextLoadSvc.interrupt();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to interrupt the load job \"{}\"", nextLoadSvc
				);
			}
		}
		super.interruptActually();
	}
	//
	@Override
	protected void shutdownActually() {
		for(final HttpDataLoadSvc<T> nextLoadSvc : loadSvcMap.values()) {
			try {
				nextLoadSvc.shutdown();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to shutdown the load job \"{}\"", nextLoadSvc
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
			loadSvcMap.size() + 1, new GroupThreadFactory("await<" + getName() + ">", true)
		);
		for(final HttpDataLoadSvc<T> nextLoadSvc : loadSvcMap.values()) {
			awaitExecutor.submit(
				new Runnable() {
					@Override
					public final void run() {
						try {
							nextLoadSvc.await(timeOut, timeUnit);
						} catch(final RemoteException e) {
							LogUtil.exception(
								LOG, Level.ERROR, e, "Failed to await the load job \"{}\"",
								nextLoadSvc
							);
						} catch(final InterruptedException e) {
							LOG.debug(Markers.MSG, "{}: await call interrupted", nextLoadSvc);
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
						BasicMixedHttpDataLoadSvc.super.await(timeOut, timeUnit);
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
		for(final HttpDataLoadSvc<T> nextLoadSvc : loadSvcMap.values()) {
			try {
				nextLoadSvc.close();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to close the load job \"{}\"", nextLoadSvc
				);
			}
		}
		super.closeActually();
	}
	//
	@Override
	public final String getWrappedLoadSvcNameFor(final LoadType loadType)
	throws RemoteException {
		final HttpDataLoadSvc<T> wrappedLoadSvc = loadSvcMap.get(loadType);
		return wrappedLoadSvc == null ? null : wrappedLoadSvc.getName();
	}
}
