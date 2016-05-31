package com.emc.mongoose.core.impl.io.task;
//
import com.emc.mongoose.common.conf.enums.LoadType;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.io.conf.IoConfig;
import com.emc.mongoose.core.api.io.task.IoTask;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 20.10.15.
 */
public class BasicIoTask<T extends Item, C extends Container<?>, X extends IoConfig<?, C>>
implements IoTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final X ioConfig;
	protected final LoadType ioType;
	protected final T item;
	protected final String nodeAddr;
	//
	protected volatile IoTask.Status status = IoTask.Status.FAIL_UNKNOWN;
	protected volatile long
		reqTimeStart = 0, reqTimeDone = 0,
		respTimeStart = 0, respDataTimeStart = 0, respTimeDone = 0,
		countBytesDone = 0;
	//
	public BasicIoTask(final T item, final String nodeAddr, final X ioConfig) {
		this.ioConfig = ioConfig;
		this.ioType = ioConfig.getLoadType();
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
	public final LoadType getLoadType() {
		return ioType;
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
	public final long getCountBytesDone() {
		return countBytesDone;
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
	@Override
	public final int getDataLatency() {
		return respDataTimeStart > reqTimeDone ? (int) (respDataTimeStart - reqTimeDone) : -1;
	}
}
