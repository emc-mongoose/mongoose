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
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by andrey on 12.10.14.
 */
public class BasicIOTask<T extends DataObject>
implements IOTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected volatile RequestConfig<T> reqConf = null;
	protected volatile String nodeAddr = null;
	protected volatile T dataItem = null;
	protected volatile Status status = Status.FAIL_UNKNOWN;
	//
	protected volatile long reqTimeStart = 0, reqTimeDone = 0, respTimeStart = 0, respTimeDone = 0;
	protected volatile long transferSize = 0;
	private volatile Type type;
	// BEGIN pool related things
	private final static InstancePool<BasicIOTask>
		POOL_BASIC_IO_TASKS = new InstancePool<>(BasicIOTask.class);
	//
	@SuppressWarnings("unchecked")
	public static <T extends DataObject> BasicIOTask<T> getInstanceFor(
		final RequestConfig<T> reqConf, final T dataItem, final String nodeAddr
	) throws InterruptedException {
		return (BasicIOTask<T>) POOL_BASIC_IO_TASKS.take(reqConf, dataItem, nodeAddr);
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
			setRequestConfig((RequestConfig<T>) args[0]);
		}
		if(args.length > 1) {
			setDataItem((T) args[1]);
		}
		if(args.length > 2) {
			setNodeAddr(String.class.cast(args[2]));
		}
		return this;
	}
	//
	@Override
	public final int compareTo(final IOTask<T> o) {
		return o == null ? -1 : hashCode() - o.hashCode();
	}
	// END pool related things
	@Override
	public final void complete() {
		final String dataItemId = dataItem.getId();
		if(
			respTimeDone < respTimeStart ||
			respTimeStart < reqTimeDone ||
			reqTimeDone < reqTimeStart
		) {
			LOG.debug(
				LogUtil.ERR, String.format(
					FMT_PERF_TRACE_INVALID, nodeAddr, dataItemId == null ? Constants.EMPTY : dataItemId,
					transferSize, status.code,
					reqTimeStart, reqTimeDone, respTimeStart, respTimeDone
				)
			);
		} else {
			LOG.info(
				LogUtil.PERF_TRACE, String.format(
					FMT_PERF_TRACE, nodeAddr, dataItemId == null ? Constants.EMPTY : dataItemId,
					transferSize, status.code,
					reqTimeStart, respTimeStart - reqTimeDone, respTimeDone - reqTimeStart
				)
			);
		}
	}
	//
	@Override
	public IOTask<T> setRequestConfig(final RequestConfig<T> reqConf) {
		this.reqConf = reqConf;
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
