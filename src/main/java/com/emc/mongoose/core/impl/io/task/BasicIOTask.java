package com.emc.mongoose.core.impl.io.task;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.req.RequestConfig;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
//
import java.rmi.RemoteException;
/**
 Created by andrey on 12.10.14.
 */
public class BasicIOTask<T extends DataItem>
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
	protected final LoadExecutor<T> loadExecutor;
	protected final T dataItem;
	protected final String nodeAddr;
	//
	protected volatile Status status = Status.FAIL_UNKNOWN;
	protected volatile long
		reqTimeStart = 0, reqTimeDone = 0, respTimeStart = 0, respTimeDone = 0, transferSize = 0;
	//
	public BasicIOTask(
		final LoadExecutor<T> loadExecutor, final T dataItem, final String nodeAddr
	) {
		try {
			this.reqConf = loadExecutor.getRequestConfig();
		} catch(final RemoteException e) {
			throw new RuntimeException(e);
		}
		this.loadExecutor = loadExecutor;
		this.dataItem = dataItem;
		this.nodeAddr = nodeAddr;
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
	public final String getNodeAddr() {
		return nodeAddr;
	}
	//
	@Override
	public final long getTransferSize() {
		return transferSize;
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
