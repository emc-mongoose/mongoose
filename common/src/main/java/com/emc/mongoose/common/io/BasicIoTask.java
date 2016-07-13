package com.emc.mongoose.common.io;

import com.emc.mongoose.common.config.LoadType;
import com.emc.mongoose.common.item.Item;

import static com.emc.mongoose.common.item.Item.SLASH;

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
	//
	protected final static ThreadLocal<StringBuilder> STRB = new ThreadLocal<StringBuilder>() {
		@Override
		protected final StringBuilder initialValue() {
			return new StringBuilder();
		}
	};
	//
	@Override
	public String toString() {
		final StringBuilder strb = STRB.get();
		strb.setLength(0);
		final String itemPath = item.getPath();
		final long respLatency = getLatency();
		final long reqDuration = getDuration();
		return strb
			.append(ioType.ordinal()).append(',')
			.append(
				itemPath == null ?
					item.getName() :
					itemPath.endsWith(SLASH) ?
						itemPath + item.getName() :
						itemPath + SLASH + item.getName()
			)
			.append(',')
			.append(status.code).append(',')
			.append(reqTimeStart).append(',')
			.append(respLatency > 0 ? respLatency : 0).append(',')
			.append(reqDuration)
			.toString();
	}
}
