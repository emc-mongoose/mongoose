package com.emc.mongoose.core.impl.io.task;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.MutableDataItem;
import com.emc.mongoose.core.api.io.req.RequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by andrey on 12.10.14.
 */
public class BasicIOTask<T extends MutableDataItem>
implements IOTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final RequestConfig<T> reqConf;
	protected final IOTask.Type ioType;
	protected final T dataItem;
	protected final long contentSize;
	protected final String nodeAddr;
	//
	protected volatile Status status = Status.FAIL_UNKNOWN;
	protected volatile long
		reqTimeStart = 0, reqTimeDone = 0, respTimeStart = 0, respTimeDone = 0,
		countBytesDone = 0, countBytesSkipped = 0;
	protected volatile DataItem currRange = null;
	protected volatile long currRangeSize = 0, nextRangeOffset = 0;
	protected volatile int currRangeIdx = 0, currDataLayerIdx = 0;
	//
	public BasicIOTask(final T dataItem, final String nodeAddr, final RequestConfig<T> reqConf) {
		this.reqConf = reqConf;
		this.ioType = reqConf.getLoadType();
		//
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
