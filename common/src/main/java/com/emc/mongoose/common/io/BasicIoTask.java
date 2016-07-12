package com.emc.mongoose.common.io;

import com.emc.mongoose.common.config.LoadType;
import com.emc.mongoose.common.item.Item;
/**
 Created by kurila on 20.10.15.
 */
public class BasicIoTask<I extends Item>
implements IoTask<I> {
	//
	protected final LoadType ioType;
	protected final I item;
	//
	protected volatile IoTask.Status status = IoTask.Status.FAIL_UNKNOWN;
	protected volatile long reqTimeStart = 0;
	protected volatile long reqTimeDone = 0;
	protected volatile long respTimeStart = 0;
	protected volatile long respTimeDone = 0;
	//
	public BasicIoTask(final LoadType ioType, final I item) {
		this.ioType = ioType;
		this.item = item;
	}
	//
	@Override
	public final I getItem() {
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
	public final int getDuration() {
		return (int) (respTimeDone - reqTimeStart);
	}
	//
	@Override
	public final int getLatency() {
		return (int) (respTimeStart - reqTimeDone);
	}
}
