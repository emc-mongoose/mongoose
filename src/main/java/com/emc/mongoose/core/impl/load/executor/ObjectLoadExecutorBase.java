package com.emc.mongoose.core.impl.load.executor;
// mongoose-core-api.jar
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.data.model.DataItemSrc;
import com.emc.mongoose.core.api.io.req.ObjectRequestConfig;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.io.task.DataObjectIOTask;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.executor.ObjectLoadExecutor;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.io.task.BasicObjectIOTask;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 09.10.14.
 */
public abstract class ObjectLoadExecutorBase<T extends DataObject>
extends TypeSpecificLoadExecutorBase<T>
implements ObjectLoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected ObjectLoadExecutorBase(
		final RunTimeConfig runTimeConfig, final ObjectRequestConfig<T> reqConfig, final String[] addrs,
		final int connCountPerNode, final int threadCount,
		final DataItemSrc<T> itemSrc, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias,
		final int manualTaskSleepMicroSecs, final float rateLimit, final int countUpdPerReq
	) {
		super(
			runTimeConfig, reqConfig, addrs, connCountPerNode, threadCount, itemSrc, maxCount,
			sizeMin, sizeMax, sizeBias, manualTaskSleepMicroSecs, rateLimit, countUpdPerReq
		);
	}
	//
	@Override
	protected DataObjectIOTask<T> getIOTask(final T dataItem, final String nextNodeAddr) {
		return new BasicObjectIOTask<>(
			dataItem, nextNodeAddr, (ObjectRequestConfig<T>) reqConfigCopy
		);
	}
	//
	@Override
	protected final void logTaskTrace(
		final T dataItem, final IOTask.Status status, final String nodeAddr,
		final long countBytesDone, final long reqTimeStart,
		final int reqDuration, final int respLatency
	) {
		if(LOG.isInfoEnabled(Markers.PERF_TRACE)) {
			final String dataItemId = dataItem.getId();
			StringBuilder strBuilder = PERF_TRACE_MSG_BUILDER.get();
			if(strBuilder == null) {
				strBuilder = new StringBuilder();
				PERF_TRACE_MSG_BUILDER.set(strBuilder);
			} else {
				strBuilder.setLength(0); // clear/reset
			}
			LOG.info(
				Markers.PERF_TRACE,
				strBuilder
					.append(nodeAddr).append(',')
					.append(dataItemId).append(',')
					.append(countBytesDone).append(',')
					.append(status.code).append(',')
					.append(reqTimeStart).append(',')
					.append(respLatency).append(',')
					.append(reqDuration)
					.toString()
			);
		}
	}

}
