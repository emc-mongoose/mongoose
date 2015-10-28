package com.emc.mongoose.core.impl.io.task;
//
import com.emc.mongoose.core.api.Item;
import com.emc.mongoose.core.api.io.req.RequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 20.10.15.
 */
public class BasicIOTask<T extends Item>
implements IOTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final RequestConfig reqConf;
	protected final IOTask.Type ioType;
	protected final T item;
	protected final String nodeAddr;
	//
	protected volatile IOTask.Status status = IOTask.Status.FAIL_UNKNOWN;
	protected volatile long
		reqTimeStart = 0, reqTimeDone = 0,
		respTimeStart = 0, respTimeDone = 0,
		countBytesDone = 0;
	//
	public BasicIOTask(
		final T item, final String nodeAddr, final RequestConfig reqConf
	) {
		this.reqConf = reqConf;
		this.ioType = reqConf.getLoadType();
		this.item = item;
		this.nodeAddr = nodeAddr;
	}
	//
	@Override
	public final String getNodeAddr() {
		return nodeAddr;
	}
	//
	@Override
	public final T getItem() {
		return item;
	}
	//
	@Override
	public long getCountBytesDone() {
		return countBytesDone;
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
	@Override
	public final long getRespTimeDone() {
		return respTimeDone;
	}
}
