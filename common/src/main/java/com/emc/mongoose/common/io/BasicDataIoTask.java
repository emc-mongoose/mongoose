package com.emc.mongoose.common.io;

import com.emc.mongoose.common.config.LoadType;
import com.emc.mongoose.common.item.DataItem;

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

	public BasicDataIoTask(final LoadType ioType, final T item) {
		super(ioType, item);
		item.reset();
		//currDataLayerIdx = item.getCurrLayerIndex();
		switch(ioType) {
			case CREATE:
			case READ:
				// TODO partial read support
				contentSize = item.getSize();
				break;
			/*case UPDATE:
				if(item.hasScheduledUpdates()) {
					contentSize = item.getUpdatingRangesSize();
				} else if(item.isAppending()) {
					contentSize = item.getAppendSize();
				} else {
					contentSize = item.getSize();
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
	public int getDataLatency() {
		return respDataTimeStart > reqTimeDone ? (int) (respDataTimeStart - reqTimeDone) : -1;
	}
}
