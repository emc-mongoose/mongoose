package com.emc.mongoose.core.impl.io.task;
//
import com.emc.mongoose.common.collections.InstancePool;
import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.core.api.io.task.DataObjectIOTask;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.executor.ObjectLoadExecutor;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
/**
 Created by kurila on 23.12.14.
 */
public class BasicObjectIOTask<T extends DataObject>
extends BasicIOTask<T>
implements DataObjectIOTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicObjectIOTask(final ObjectLoadExecutor<T> loadExecutor) {
		super(loadExecutor);
	}
	//
	public final static Map<ObjectLoadExecutor, InstancePool<BasicObjectIOTask>>
		INSTANCE_POOL_MAP = new HashMap<>();
	//
	public static BasicObjectIOTask getInstance(
		final ObjectLoadExecutor loadExecutor, DataObject dataItem, final String nodeAddr
	) {
		InstancePool<BasicObjectIOTask> instPool = INSTANCE_POOL_MAP.get(loadExecutor);
		if(instPool == null) {
			try {
				instPool = new InstancePool<>(
					BasicObjectIOTask.class.getConstructor(ObjectLoadExecutor.class), loadExecutor
				);
				INSTANCE_POOL_MAP.put(loadExecutor, instPool);
			} catch(final NoSuchMethodException e) {
				throw new IllegalStateException(e);
			}
		}
		//
		return instPool.take(dataItem, nodeAddr);
	}
	//
	@Override
	public void release() {
		final InstancePool<BasicObjectIOTask> instPool = INSTANCE_POOL_MAP.get(loadExecutor);
		if(instPool == null) {
			throw new IllegalStateException("No pool found to release back");
		} else {
			instPool.release(this);
		}
	}
	//
	@Override
	public final void complete() {
		final String dataItemId = dataItem.getId();
		StringBuilder strBuilder = THRLOC_SB.get();
		if(strBuilder == null) {
			strBuilder = new StringBuilder();
			THRLOC_SB.set(strBuilder);
		} else {
			strBuilder.setLength(0); // clear/reset
		}
		if(
			respTimeDone < respTimeStart ||
				respTimeStart < reqTimeDone ||
				reqTimeDone < reqTimeStart
			) {
			LOG.debug(
				LogUtil.ERR,
				strBuilder
					.append("Invalid trace: ")
					.append(nodeAddr).append(',')
					.append(dataItemId).append(',')
					.append(transferSize).append(',')
					.append(status.code).append(',')
					.append(reqTimeStart).append(',')
					.append(reqTimeDone).append(',')
					.append(respTimeStart).append(',')
					.append(respTimeDone)
					.toString()
			);
		} else {
			LOG.info(
				LogUtil.PERF_TRACE,
				strBuilder
					.append(nodeAddr).append(',')
					.append(dataItemId).append(',')
					.append(transferSize).append(',')
					.append(status.code).append(',')
					.append(reqTimeStart).append(',')
					.append(respTimeStart - reqTimeDone).append(',')
					.append(respTimeDone - reqTimeStart)
					.toString()
			);
		}
		//
		try {
			loadExecutor.handleResult(this);
		} catch(final RemoteException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Unexpected network failure");
		}
		//
		final int reqSleepMilliSec = reqConf.getReqSleepMilliSec();
		if(reqSleepMilliSec > 0) {
			try {
				Thread.sleep(reqSleepMilliSec);
			} catch(final InterruptedException e) {
				LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted request sleep");
			}
		}
		//
		release();
	}
}
