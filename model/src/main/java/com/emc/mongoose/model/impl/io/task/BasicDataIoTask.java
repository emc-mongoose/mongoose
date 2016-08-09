package com.emc.mongoose.model.impl.io.task;

import com.emc.mongoose.model.api.io.task.DataIoTask;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.util.LoadType;

import java.io.IOException;

public class BasicDataIoTask<T extends DataItem>
extends BasicIoTask<T>
implements DataIoTask<T> {

	protected final long contentSize;
	protected volatile long countBytesDone = 0;
	protected volatile DataItem currRange = null;
	protected volatile long currRangeSize = 0;
	protected volatile long nextRangeOffset = 0;
	protected volatile int currRangeIdx = 0;
	protected volatile int currDataLayerIdx = 0;
	protected volatile long respDataTimeStart = 0;

	public BasicDataIoTask(final LoadType ioType, final T item, final String dstPath)
	throws IOException {
		super(ioType, item, dstPath);
		item.reset();
		//currDataLayerIdx = item.getCurrLayerIndex();
		switch(ioType) {
			case CREATE:
			case READ:
				// TODO partial read support
				contentSize = item.size();
				break;
			/*case UPDATE:
				if(item.hasScheduledUpdates()) {
					contentSize = item.getUpdatingRangesSize();
				} else if(item.isAppending()) {
					contentSize = item.getAppendSize();
				} else {
					contentSize = item.size();
				}
				break;*/
			default:
				contentSize = 0;
				break;
		}
	}

	@Override
	public final long getCountBytesDone() {
		return countBytesDone;
	}

	@Override
	public final void setCountBytesDone(final long n) {
		this.countBytesDone = n;
		if(contentSize == countBytesDone) {
			status = Status.SUCC;
		}
	}

	@Override
	public int getDataLatency() {
		return respDataTimeStart > reqTimeDone ? (int) (respDataTimeStart - reqTimeDone) : -1;
	}

	@Override @SuppressWarnings("ResultOfMethodCallIgnored")
	public String toString() {
		super.toString(); // invoked to fill the string builder
		final StringBuilder strb = STRB.get();
		return strb
			.append(',').append(countBytesDone)
			.append(',').append(getDataLatency())
			.toString();
	}
}
