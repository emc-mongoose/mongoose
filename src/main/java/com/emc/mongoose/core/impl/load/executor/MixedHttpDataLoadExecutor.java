package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.DataRangesConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.conf.enums.LoadType;
//
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.executor.HttpDataLoadExecutor;
import com.emc.mongoose.core.api.load.model.FaceControl;
//
import com.emc.mongoose.core.impl.load.model.WeightFaceControl;
//
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
/**
 Created by kurila on 29.03.16.
 */
public class MixedHttpDataLoadExecutor<T extends HttpDataItem>
extends BasicHttpDataLoadExecutor<T>
implements HttpDataLoadExecutor<T> {
	//
	private final FaceControl faceControl;
	private final Map<LoadType, HttpRequestConfig<T, ? extends Container<T>>>
		reqConfigMap = new HashMap<>();
	private final Map<LoadType, HttpDataLoadExecutor<T>>
		loadExecutorMap = new HashMap<>();
	//
	public MixedHttpDataLoadExecutor(
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
		this.faceControl = new WeightFaceControl<>(loadTypeWeightMap);
		for(final LoadType loadType : loadTypeWeightMap.keySet()) {
			final HttpRequestConfig<T, ? extends Container<T>> reqConfigCopy;
			try {
				reqConfigCopy = (HttpRequestConfig<T, ? extends Container<T>>) reqConfig
					.clone().setLoadType(loadType);
			} catch(final CloneNotSupportedException e) {
				throw new IllegalStateException(e);
			}
			reqConfigMap.put(loadType, reqConfigCopy);
			final HttpDataLoadExecutor<T> nextLoadExecutor = new BasicHttpDataLoadExecutor<T>(
				appConfig, reqConfigCopy, addrs, threadCount, itemSrcMap.get(loadType),
				maxCount, rateLimit, sizeConfig, rangesConfig, null, null, null, null
			) {
				@Override
				public final <A extends IOTask<T>> Future<A> submitTask(final A ioTask)
				throws RejectedExecutionException {
					return MixedHttpDataLoadExecutor.this.submitTask(ioTask);
				}
				//
				@Override
				public final <A extends IOTask<T>> int submitTasks(
					final List<A> ioTasks, int from, int to
				) throws RejectedExecutionException {
					return MixedHttpDataLoadExecutor.this.submitTasks(ioTasks, from, to);
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
			if(faceControl.requestApprovalFor(ioTask)) {
				return super.submitTask(ioTask);
			} else {
				throw new RejectedExecutionException(
					"Face control rejected the task #" + ioTask.hashCode()
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
			if(faceControl.requestBatchApprovalFor(ioTasks, from, to)) {
				return super.submitTasks(ioTasks, from, to);
			} else {
				throw new RejectedExecutionException(
					"Face control rejected " + (to - from) + " tasks"
				);
			}
		} catch(final InterruptedException e) {
			throw new RejectedExecutionException(e);
		}
	}
}
