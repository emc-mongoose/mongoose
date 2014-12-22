package com.emc.mongoose.base.api.impl;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.AppendableDataItem;
import com.emc.mongoose.base.data.UpdatableDataItem;
import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.pool.InstancePool;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/**
 Created by andrey on 12.10.14.
 */
public class IOTaskBase<T extends DataObject>
implements AsyncIOTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static IOTaskBase POISON = new IOTaskBase() {
		@Override
		public final String toString() {
			return "<POISON>";
		}
	};
	//
	protected volatile RequestConfig<T> reqConf = null;
	protected volatile String nodeAddr = null;
	protected volatile T dataItem = null;
	protected volatile Result result = Result.FAIL_TIMEOUT;
	//
	protected volatile long reqTimeStart = 0, reqTimeDone = 0, respTimeStart = 0, respTimeDone = 0;
	private volatile long transferSize = 0;
	private volatile Type type;
	//
	protected final Lock lock = new ReentrantLock();
	protected final Condition condDone = lock.newCondition();
	//
	public IOTaskBase() {

	}
	// BEGIN pool related things
	@Override
	public final void close() {
		final InstancePool<AsyncIOTask> pool = POOL_MAP.get(reqConf);
		reset();
		pool.release(this);
	}
	//
	@Override
	public void reset() {
		result = Result.FAIL_TIMEOUT;
		reqTimeStart = 0;
		reqTimeDone = 0;
		respTimeStart = 0;
		respTimeDone = 0;
		transferSize = 0;
	}
	// END pool related things
	@Override
	public final void complete() {
		LOG.info(
			Markers.PERF_TRACE, String.format(
				FMT_PERF_TRACE, dataItem.getId(), dataItem.getSize(), result.code,
				reqTimeStart, reqTimeDone - reqTimeStart, respTimeStart - reqTimeDone,
				respTimeDone - respTimeStart
			)
		);
	}
	//
	@Override
	public AsyncIOTask<T> setRequestConfig(final RequestConfig<T> reqConf) {
		this.reqConf = reqConf;
		type = reqConf.getLoadType();
		return this;
	}
	//
	@Override
	public AsyncIOTask<T> setNodeAddr(final String nodeAddr) {
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
	public AsyncIOTask<T> setDataItem(final T dataItem) {
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
	public final Result getResult() {
		return result;
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
