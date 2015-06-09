package com.emc.mongoose.core.impl.io.task;
//
import com.emc.mongoose.common.collections.InstancePool;
import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.common.logging.Markers;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
import com.emc.mongoose.core.api.data.AppendableDataItem;
import com.emc.mongoose.core.api.data.UpdatableDataItem;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
/**
 Created by andrey on 12.10.14.
 */
public class BasicIOTask<T extends DataItem>
implements IOTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final static ThreadLocal<StringBuilder> THRLOC_SB = new ThreadLocal<StringBuilder>() {
		@Override
		protected final StringBuilder initialValue() {
			return new StringBuilder();
		}
	};
	//
	protected final LoadExecutor<T> loadExecutor;
	protected final RequestConfig<T> reqConf;
	//
	protected volatile String nodeAddr = null;
	protected volatile T dataItem = null;
	protected volatile Status status = Status.FAIL_UNKNOWN;
	//
	protected volatile long reqTimeStart = 0, reqTimeDone = 0, respTimeStart = 0, respTimeDone = 0;
	protected volatile long transferSize = 0;
	//
	public BasicIOTask(final LoadExecutor<T> loadExecutor) {
		this.loadExecutor = loadExecutor;
		try {
			this.reqConf = loadExecutor.getRequestConfig();
		} catch(final RemoteException e) {
			throw new IllegalStateException(e);
		}
	}
	//
	public final static Map<LoadExecutor, InstancePool<BasicIOTask>>
		INSTANCE_POOL_MAP = new HashMap<>();
	//
	public static BasicIOTask getInstance(
		final LoadExecutor loadExecutor, DataItem dataItem, final String nodeAddr
	) {
		InstancePool<BasicIOTask> instPool = INSTANCE_POOL_MAP.get(loadExecutor);
		if(instPool == null) {
			try {
				instPool = new InstancePool<>(
					BasicIOTask.class.getConstructor(LoadExecutor.class), loadExecutor
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
	@Override @SuppressWarnings("unchecked")
	public final BasicIOTask<T> reuse(final Object... args) {
		if(args == null) {
			throw new IllegalArgumentException("No args for reusing");
		} else {
			if(args.length > 0) {
				setDataItem((T) args[0]);
			}
			if(args.length > 1) {
				setNodeAddr(String.class.cast(args[1]));
			}
		}
		return this;
	}
	//
	@Override
	public void release() {
		final InstancePool<BasicIOTask> instPool = INSTANCE_POOL_MAP.get(loadExecutor);
		if(instPool == null) {
			throw new IllegalStateException("No pool found to release back");
		} else {
			instPool.release(this);
		}
	}
	//
	@Override
	public void complete() {
		final String dataItemId = Long.toHexString(dataItem.getOffset());
		StringBuilder strBuilder = THRLOC_SB.get();
		if(strBuilder == null) {
			strBuilder = new StringBuilder();
			THRLOC_SB.set(strBuilder);
		} else {
			strBuilder.setLength(0); // clear/reset
		}
		if(
			reqTimeDone >= reqTimeStart ||
				respTimeStart >= reqTimeDone ||
				respTimeDone >= respTimeStart
			) {
			LOG.info(
				Markers.PERF_TRACE,
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
		} else if(
			status != Status.CANCELLED &&
			status != Status.FAIL_IO &&
			status != Status.FAIL_TIMEOUT &&
			status != Status.FAIL_UNKNOWN
		) {
			LOG.warn(
				Markers.ERR,
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
	}
	//
	@Override
	public IOTask<T> setNodeAddr(final String nodeAddr) {
		this.nodeAddr = nodeAddr;
		return this;
	}
	//
	@Override
	public final String getNodeAddr() {
		return nodeAddr;
	}
	//
	@Override
	public IOTask<T> setDataItem(final T dataItem) {
		this.dataItem = dataItem;
		final Type loadType = reqConf.getLoadType();
		switch(loadType) {
			case APPEND:
				transferSize = AppendableDataItem.class.cast(dataItem).getPendingAugmentSize();
				break;
			case UPDATE:
				transferSize = UpdatableDataItem.class.cast(dataItem).getPendingRangesSize();
				break;
			default:
				transferSize = dataItem.getSize();
		}
		return this;
	}
	//
	@Override
	public final T getDataItem() {
		return dataItem;
	}
	//
	@Override
	public final long getTransferSize() {
		return transferSize;
	}
	//
	@Override
	public final Status getStatus() {
		return status;
	}
	//
	@Override
	public final int getLatency() {
		return (int) (respTimeStart - reqTimeDone);
	}
	//
	@Override
	public final long getReqTimeStart() {
		return reqTimeStart;
	}
	//
	@Override
	public final long getReqTimeDone() {
		return reqTimeDone;
	}
	//
	@Override
	public final long getRespTimeStart() {
		return respTimeStart;
	}
	//
	@Override
	public final long getRespTimeDone() {
		return respTimeDone;
	}
}
