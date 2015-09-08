package com.emc.mongoose.core.impl.io.task;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.AppendableDataItem;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.UpdatableDataItem;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.req.RequestConfig;
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
public class BasicIOTask<T extends UpdatableDataItem & AppendableDataItem>
implements IOTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	protected final static ThreadLocal<StringBuilder> THRLOC_SB = new ThreadLocal<StringBuilder>() {
		@Override
		protected final StringBuilder initialValue() {
			return new StringBuilder();
		}
	};
	//
	protected final RequestConfig<T> reqConf;
	protected final IOTask.Type ioType;
	protected final LoadExecutor<T> loadExecutor;
	protected final T dataItem;
	protected final long contentSize;
	protected final String nodeAddr;
	//
	protected volatile Status status = Status.FAIL_UNKNOWN;
	protected volatile long
		reqTimeStart = 0, reqTimeDone = 0, respTimeStart = 0, respTimeDone = 0, countBytesDone = 0;
	protected volatile DataItem currRange = null;
	protected volatile long currRangeSize = 0, nextRangeOffset = 0;
	protected volatile int currRangeIdx = 0, currDataLayerIdx = 0;
	//
	public BasicIOTask(
		final LoadExecutor<T> loadExecutor, final T dataItem, final String nodeAddr
	) {
		try {
			this.reqConf = loadExecutor.getRequestConfig();
			this.ioType = reqConf.getLoadType();
		} catch(final RemoteException e) {
			throw new RuntimeException(e);
		}
		//
		this.loadExecutor = loadExecutor;
		this.dataItem = dataItem;
		dataItem.reset();
		currDataLayerIdx = dataItem.getCurrLayerIndex();
		switch(ioType) {
			case CREATE:
				contentSize = dataItem.getSize();
				break;
			case READ:
				contentSize = dataItem.getSize();
				break;
			case DELETE:
				contentSize = 0;
				break;
			case UPDATE:
				contentSize = dataItem.getUpdatingRangesSize();
				break;
			case APPEND:
				contentSize = dataItem.getAppendSize();
				break;
			default:
				contentSize = 0;
				break;
		}
		this.nodeAddr = nodeAddr;
	}
	//
	@Override
	public void complete() {
		//
		if(reqTimeDone == 0) {
			respTimeDone = System.nanoTime() / 1000;
		}
		//
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
					.append(countBytesDone).append(',')
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
					.append(countBytesDone).append(',')
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
	public final String getNodeAddr() {
		return nodeAddr;
	}
	//
	@Override
	public final long getCountBytesDone() {
		return countBytesDone;
	}
	//
	@Override
	public final T getDataItem() {
		return dataItem;
	}
	//
	@Override
	public final Status getStatus() {
		return status;
	}
	//
	@Override
	public final int getDuration() {
		return (int) (respTimeDone - reqTimeStart);
	}
	//
	@Override
	public final int getLatency() {
		return (int) (respTimeStart - reqTimeDone);
	}
	//
}
