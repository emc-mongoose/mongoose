package com.emc.mongoose.core.impl.io.task;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.collections.InstancePool;
//
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
import com.emc.mongoose.core.api.data.AppendableDataItem;
import com.emc.mongoose.core.api.data.UpdatableDataItem;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
/**
 Created by andrey on 12.10.14.
 */
public class BasicIOTask<T extends DataObject>
implements IOTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static ThreadLocal<StringBuilder> THRLOC_SB = new ThreadLocal<StringBuilder>() {
		@Override
		protected final StringBuilder initialValue() {
			return new StringBuilder();
		}
	};
	//
	protected volatile LoadExecutor<T> loadExecutor = null;
	protected volatile RequestConfig<T> reqConf = null;
	protected volatile String nodeAddr = null;
	protected volatile T dataItem = null;
	protected volatile Status status = Status.FAIL_UNKNOWN;
	//
	protected volatile long reqTimeStart = 0, reqTimeDone = 0, respTimeStart = 0, respTimeDone = 0;
	protected volatile long transferSize = 0;
	protected int reqSleepMilliSec = 0;
	private volatile Type type;
	// BEGIN pool related things
	private final static InstancePool<BasicIOTask>
		POOL_BASIC_IO_TASKS = new InstancePool<>(BasicIOTask.class);
	//
	@SuppressWarnings("unchecked")
	public static <T extends DataObject> BasicIOTask<T> getInstanceFor(
		final LoadExecutor<T> loadExecutor, final T dataItem, final String nodeAddr
	) {
		return (BasicIOTask<T>) POOL_BASIC_IO_TASKS.take(loadExecutor, dataItem, nodeAddr);
	}
	//
	@Override
	public void release() {
		POOL_BASIC_IO_TASKS.release(this);
	}
	//
	@Override @SuppressWarnings("unchecked")
	public BasicIOTask<T> reuse(final Object... args)
	throws IllegalStateException {
		status = Status.FAIL_UNKNOWN;
		reqTimeStart = reqTimeDone = respTimeStart = respTimeDone = transferSize = 0;
		if(args.length > 0) {
			setLoadExecutor((LoadExecutor<T>)args[0]);
		}
		if(args.length > 1) {
			setDataItem((T) args[1]);
		}
		if(args.length > 2) {
			setNodeAddr(String.class.cast(args[2]));
		}
		return this;
	}
	// END pool related things
	@Override
	public final void complete() {
		final String dataItemId = dataItem.getId();
		StringBuilder strBuilder = THRLOC_SB.get();
		strBuilder.setLength(0); // clear/reset
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
					.append(dataItemId == null ? Constants.EMPTY : dataItemId).append(',')
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
					.append(dataItemId == null ? Constants.EMPTY : dataItemId).append(',')
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
	public IOTask<T> setLoadExecutor(final LoadExecutor<T> loadExecutor) {
		this.loadExecutor = loadExecutor;
		try {
			setRequestConfig(loadExecutor.getRequestConfig());
		} catch(final RemoteException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Unexpected network failure");
		}
		return this;
	}
	//
	@Override
	public IOTask<T> setRequestConfig(final RequestConfig<T> reqConf) {
		this.reqConf = reqConf;
		reqSleepMilliSec = reqConf.getReqSleepMilliSec();
		type = reqConf.getLoadType();
		return this;
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
		switch(type) {
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
