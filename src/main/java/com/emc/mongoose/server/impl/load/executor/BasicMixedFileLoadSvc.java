package com.emc.mongoose.server.impl.load.executor;
//
import com.emc.mongoose.common.concurrent.NamingThreadFactory;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.DataRangesConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.core.api.io.conf.FileIoConfig;
import com.emc.mongoose.core.api.io.task.IoTask;
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.load.model.Throttle;
import com.emc.mongoose.core.api.load.executor.FileLoadExecutor;
import com.emc.mongoose.core.api.load.model.metrics.IoStats;
import com.emc.mongoose.core.impl.load.model.WeightThrottle;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 05.04.16.
 */
public class BasicMixedFileLoadSvc<F extends FileItem>
extends BasicFileLoadSvc<F>
implements MixedFileLoadSvc<F> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Throttle<LoadType> throttle;
	private final Map<LoadType, Integer> loadTypeWeights;
	private final Map<LoadType, FileIoConfig<F, ? extends Directory<F>>>
		reqConfigMap = new HashMap<>();
	protected final Map<LoadType, FileLoadSvc<F>>
		loadSvcMap = new HashMap<>();
	
	//
	public BasicMixedFileLoadSvc(
		final AppConfig appConfig, final FileIoConfig<F, ? extends Directory<F>> reqConfig,
		final int threadCount, final long countLimit, final long sizeLimit, final float rateLimit,
		final SizeInBytes sizeConfig, final DataRangesConfig rangesConfig,
		final Map<LoadType, Integer> loadTypeWeightMap,
		final Map<LoadType, Input<F>> itemInput
	) {
		super(
			appConfig, reqConfig, threadCount, null, countLimit, sizeLimit, rateLimit, sizeConfig,
			rangesConfig
		);
		//
		this.loadTypeWeights = loadTypeWeightMap;
		this.throttle = new WeightThrottle<>(loadTypeWeights, isInterrupted);
		for(final LoadType nextLoadType : loadTypeWeights.keySet()) {
			final FileIoConfig<F, ? extends Directory<F>> reqConfigCopy;
			try {
				reqConfigCopy = (FileIoConfig<F, ? extends Directory<F>>) reqConfig
					.clone().setLoadType(nextLoadType);
			} catch(final CloneNotSupportedException e) {
				throw new IllegalStateException(e);
			}
			reqConfigMap.put(nextLoadType, reqConfigCopy);
			final BasicFileLoadSvc<F> nextLoadSvc = new BasicFileLoadSvc<F>(
				appConfig, reqConfigCopy, threadCount, null, countLimit,
				sizeLimit, rateLimit, sizeConfig, rangesConfig
			) {
				@Override
				public final <A extends IoTask<F>> Future<A> submitTask(final A ioTask)
				throws RejectedExecutionException {
					try {
						if(throttle.requestContinueFor(nextLoadType)) {
							return BasicMixedFileLoadSvc.this.submitTask(ioTask);
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
				public final <A extends IoTask<F>> int submitTasks(
					final List<A> ioTasks, int from, int to
				) throws RejectedExecutionException {
					try {
						if(throttle.requestContinueFor(nextLoadType, to - from)) {
							return BasicMixedFileLoadSvc.this.submitTasks(
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
	public final void ioTaskCompleted(final IoTask<F> ioTask)
	throws RemoteException {
		loadSvcMap.get(ioTask.getLoadType())
			.ioTaskCompleted(ioTask);
		super.ioTaskCompleted(ioTask);
	}
	//
	@Override
	public final int ioTaskCompletedBatch(
		final List<? extends IoTask<F>> ioTasks, final int from, final int to
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
		FileLoadExecutor<F> nextLoadJob;
		int nextLoadWeight;
		IoStats.Snapshot nextLoadStats = null;
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
		for(final FileLoadSvc<F> nextLoadSvc : loadSvcMap.values()) {
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
		for(final FileLoadSvc<F> nextLoadSvc : loadSvcMap.values()) {
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
		for(final FileLoadSvc<F> nextLoadSvc : loadSvcMap.values()) {
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
	public boolean await(final long timeOut, final TimeUnit timeUnit)
	throws InterruptedException, RemoteException {
		final ExecutorService awaitExecutor = Executors.newFixedThreadPool(
			loadSvcMap.size() + 1, new NamingThreadFactory("await<" + getName() + ">", true)
		);
		for(final FileLoadSvc<F> nextLoadSvc : loadSvcMap.values()) {
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
						BasicMixedFileLoadSvc.super.await(timeOut, timeUnit);
					} catch(final InterruptedException e) {
						LOG.debug(Markers.MSG, "{}: await call interrupted", getName());
					} catch(final RemoteException e) {
						LogUtil.exception(LOG, Level.ERROR, e, "Remote await method call failure");
					}
				}
			}
		);
		awaitExecutor.shutdown();
		try {
			return awaitExecutor.awaitTermination(timeOut, timeUnit);
		} finally {
			awaitExecutor.shutdownNow();
		}
	}
	//
	@Override
	protected void closeActually()
	throws IOException {
		for(final FileLoadSvc<F> nextLoadSvc : loadSvcMap.values()) {
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
		final FileLoadSvc<F> wrappedLoadSvc = loadSvcMap.get(loadType);
		return wrappedLoadSvc == null ? null : wrappedLoadSvc.getName();
	}
}
