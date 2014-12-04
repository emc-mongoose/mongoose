package com.emc.mongoose.base.api.impl;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.AppendableDataItem;
import com.emc.mongoose.base.data.UpdatableDataItem;
import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.pool.BasicInstancePool;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.concurrent.ConcurrentHashMap;
/**
 Created by andrey on 12.10.14.
 */
public abstract class IOTaskBase<T extends DataObject>
implements AsyncIOTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static IOTaskBase POISON = new IOTaskBase() {
		@Override
		public final void execute()
		throws InterruptedException {
			throw new InterruptedException("Attempted to eat the poison");
		}
	};
	//
	protected RequestConfig<T> reqConf = null;
	protected T dataItem = null;
	protected Result result = Result.FAIL_TIMEOUT;
	//
	protected long reqTimeStart = 0, reqTimeDone = 0, respTimeStart = 0, respTimeDone = 0;
	private long transferSize = 0;
	private Type type;

	public IOTaskBase() {

	}
	// BEGIN pool related things
	protected final static ConcurrentHashMap<RequestConfig, BasicInstancePool<AsyncIOTask>>
		POOL_MAP = new ConcurrentHashMap<>();
	//
	@Override
	public final void close() {
		final BasicInstancePool<AsyncIOTask> pool = POOL_MAP.get(reqConf);
		pool.release(this);
	}
	// END pool related things
	@Override
	public AsyncIOTask<T> setRequestConfig(final RequestConfig<T> reqConf) {
		this.reqConf = reqConf;
		type = reqConf.getLoadType();
		return this;
	}
	//
	@Override
	public final T getDataItem() {
		return dataItem;
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
	//
	@Override
	public final AsyncIOTask<T> call()
	throws Exception {
		execute();
		LOG.info(
			Markers.PERF_TRACE, String.format(
				FMT_PERF_TRACE, dataItem.getId(), dataItem.getSize(), result.code,
				reqTimeStart, reqTimeDone - reqTimeStart, respTimeStart - reqTimeDone, respTimeDone - respTimeStart
			)
		);
		return this;
	}
	//
}
