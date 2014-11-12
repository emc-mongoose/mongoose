package com.emc.mongoose.base.api.impl;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.AppendableDataItem;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.data.UpdatableDataItem;
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
public abstract class RequestBase<T extends DataItem>
implements Request<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static RequestBase POISON = new RequestBase() {
		@Override
		public final void execute()
		throws InterruptedException {
			throw new InterruptedException("Attempted to eat the poison");
		}
	};
	//
	protected RequestConfig<T> reqConf = null;
	protected T dataItem = null;
	protected Result result = Result.FAIL_UNKNOWN;
	//
	private long start = 0, duration = 0, transferSize = 0;
	private Type type;

	public RequestBase() {

	}
	// BEGIN pool related things
	protected final static ConcurrentHashMap<RequestConfig, BasicInstancePool<Request>>
		POOL_MAP = new ConcurrentHashMap<>();
	//
	@Override
	public final void close() {
		final BasicInstancePool<Request> pool = POOL_MAP.get(reqConf);
		pool.release(this);
	}
	// END pool related things
	@Override
	public Request<T> setRequestConfig(final RequestConfig<T> reqConf) {
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
	public Request<T> setDataItem(final T dataItem) {
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
	public final long getStartTime() {
		return start;
	}
	//
	@Override
	public final long getDuration() {
		return duration;
	}
	//
	@Override
	public final Request<T> call()
	throws Exception {
		start = System.nanoTime();
		execute();
		duration = System.nanoTime() - start;
		LOG.info(Markers.PERF_TRACE, "{},{},{},{}", dataItem, result.code, start, duration);
		return this;
	}
	//
}
