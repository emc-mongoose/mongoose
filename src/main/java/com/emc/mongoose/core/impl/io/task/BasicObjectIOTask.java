package com.emc.mongoose.core.impl.io.task;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.io.task.DataObjectIOTask;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.load.executor.ObjectLoadExecutor;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 23.12.14.
 */
public class BasicObjectIOTask<T extends DataObject>
extends BasicIOTask<T>
implements DataObjectIOTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicObjectIOTask(
		final ObjectLoadExecutor<T> loadExecutor, final T dataObject, final String nodeAddr
	) {
		super(loadExecutor, dataObject, nodeAddr);
	}
	//
	@Override
	public final void complete() {
		//
		if(respTimeDone == 0) {
			reqTimeDone = System.nanoTime() / 1000;
		}
		//
		final String dataItemId = dataItem.getId();
		StringBuilder strBuilder = THRLOC_SB.get();
		if(strBuilder == null) {
			strBuilder = new StringBuilder();
			THRLOC_SB.set(strBuilder);
		} else {
			strBuilder.setLength(0); // clear/reset
		}
		if(
			reqTimeDone >= reqTimeStart &&
			respTimeStart >= reqTimeDone &&
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
		final int reqSleepMilliSec = reqConf.getReqSleepMilliSec();
		if(reqSleepMilliSec > 0) {
			try {
				Thread.sleep(reqSleepMilliSec);
			} catch(final InterruptedException e) {
				LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted request sleep");
			}
		}
		//
		try {
			loadExecutor.handleResult(this);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Unexpected failure");
			e.printStackTrace(System.err);
		}
	}
}
