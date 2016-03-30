package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.DataRangesConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.executor.HttpDataLoadExecutor;
//
import com.emc.mongoose.core.impl.load.model.WeightBarrier;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 29.03.16.
 */
public class WeightedHttpDataLoadExecutor<T extends HttpDataItem>
extends BasicHttpDataLoadExecutor<T>
implements HttpDataLoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final WeightBarrier<LoadType, IOTask<T>> barrier;
	private final Map<LoadType, Integer> loadTypeWeights;
	private final Map<LoadType, HttpRequestConfig<T, ? extends Container<T>>>
		reqConfigMap = new HashMap<>();
	private final Map<LoadType, BasicHttpDataLoadExecutor<T>>
		loadExecutorMap = new HashMap<>();
	//
	public WeightedHttpDataLoadExecutor(
		final AppConfig appConfig, final HttpRequestConfig<T, ? extends Container<T>> reqConfig,
		final String[] addrs, final int threadCount, final long maxCount, final float rateLimit,
		final SizeInBytes sizeConfig, final DataRangesConfig rangesConfig,
		final Map<LoadType, Integer> loadTypeWeightMap, final Map<LoadType, ItemSrc<T>> itemSrcMap
	) {
		super(
			appConfig, reqConfig, addrs, threadCount, null, maxCount, rateLimit, sizeConfig,
			rangesConfig
		);
		//
		this.loadTypeWeights = loadTypeWeightMap;
		this.barrier = new WeightBarrier<>(loadTypeWeights);
		for(final LoadType loadType : loadTypeWeights.keySet()) {
			final HttpRequestConfig<T, ? extends Container<T>> reqConfigCopy;
			try {
				reqConfigCopy = (HttpRequestConfig<T, ? extends Container<T>>) reqConfig
					.clone().setLoadType(loadType);
			} catch(final CloneNotSupportedException e) {
				throw new IllegalStateException(e);
			}
			reqConfigMap.put(loadType, reqConfigCopy);
			final BasicHttpDataLoadExecutor<T> nextLoadExecutor = new BasicHttpDataLoadExecutor<T>(
				appConfig, reqConfigCopy, addrs, threadCount, itemSrcMap.get(loadType),
				maxCount, rateLimit, sizeConfig, rangesConfig,
				httpProcessor, client, ioReactor, connPoolMap
			) {
				@Override
				public final <A extends IOTask<T>> Future<A> submitTask(final A ioTask)
				throws RejectedExecutionException {
					return WeightedHttpDataLoadExecutor.this.submitTask(ioTask);
				}
				//
				@Override
				public final <A extends IOTask<T>> int submitTasks(
					final List<A> ioTasks, int from, int to
				) throws RejectedExecutionException {
					return WeightedHttpDataLoadExecutor.this.submitTasks(ioTasks, from, to);
				}
			};
			loadExecutorMap.put(loadType, nextLoadExecutor);
		}
	}
	//
	@Override
	public final <A extends IOTask<T>> Future<A> submitTask(final A ioTask)
	throws RejectedExecutionException {
		try {
			if(barrier.requestApprovalFor(ioTask)) {
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
	throws RejectedExecutionException {
		try {
			if(barrier.requestBatchApprovalFor((List<IOTask<T>>) ioTasks, from, to)) {
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
	protected void ioTaskCompleted(final IOTask<T> ioTask) {
		loadExecutorMap.get(ioTask.getKey()).ioTaskCompleted(ioTask);
		super.ioTaskCompleted(ioTask);
	}
	//
	@Override
	protected final int ioTaskCompletedBatch(
		final List<? extends IOTask<T>> ioTasks, final int from, final int to
	) {
		if(ioTasks != null && ioTasks.size() > 0) {
			loadExecutorMap.get(ioTasks.get(0).getKey()).ioTaskCompletedBatch(ioTasks, from, to);
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
		BasicHttpDataLoadExecutor nextLoadJob;
		int nextLoadWeight;
		for(final LoadType nextLoadType : loadExecutorMap.keySet()) {
			nextLoadWeight = loadTypeWeights.get(nextLoadType);
			nextLoadJob = loadExecutorMap.get(nextLoadType);
			strb
				.appendFixedWidthPadLeft(nextLoadWeight + " % ", 6, ' ')
				.appendFixedWidthPadLeft(nextLoadType.name() + " ", 10, ' ')
				.append(nextLoadJob.lastStats)
				.appendNewLine();
		}
		strb
			.appendPadding(100, '-').appendNewLine()
			.appendFixedWidthPadLeft("100 %     TOTAL ", 16, ' ')
			.append(lastStats)
			.appendNewLine()
			.appendPadding(100, '-');
		LOG.info(Markers.MSG, strb.toString());
	}
	//
	@Override
	protected void startActually() {
		for(final HttpDataLoadExecutor<T> nextLoadExecutor : loadExecutorMap.values()) {
			try {
				nextLoadExecutor.start();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to start the load job \"{}\"", nextLoadExecutor
				);
			}
		}
		super.startActually();
	}
	//
	@Override
	protected void interruptActually() {
		for(final HttpDataLoadExecutor<T> nextLoadExecutor : loadExecutorMap.values()) {
			try {
				nextLoadExecutor.interrupt();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to interrupt the load job \"{}\"", nextLoadExecutor
				);
			}
		}
		super.interruptActually();
	}
	//
	@Override
	protected void shutdownActually() {
		for(final HttpDataLoadExecutor<T> nextLoadExecutor : loadExecutorMap.values()) {
			try {
				nextLoadExecutor.shutdown();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to shutdown the load job \"{}\"", nextLoadExecutor
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
			loadExecutorMap.size() + 1, new GroupThreadFactory("await<" + getName() + ">", true)
		);
		for(final HttpDataLoadExecutor<T> nextLoadExecutor : loadExecutorMap.values()) {
			awaitExecutor.submit(
				new Runnable() {
					@Override
					public final void run() {
						try {
							nextLoadExecutor.await(timeOut, timeUnit);
						} catch(final RemoteException e) {
							LogUtil.exception(
								LOG, Level.ERROR, e, "Failed to await the load job \"{}\"",
								nextLoadExecutor
							);
						} catch(final InterruptedException e) {
							LOG.debug(Markers.MSG, "{}: await call interrupted", nextLoadExecutor);
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
						WeightedHttpDataLoadExecutor.super.await(timeOut, timeUnit);
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
		for(final HttpDataLoadExecutor<T> nextLoadExecutor : loadExecutorMap.values()) {
			try {
				nextLoadExecutor.close();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to close the load job \"{}\"", nextLoadExecutor
				);
			}
		}
		super.closeActually();
	}
}
