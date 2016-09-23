package com.emc.mongoose.model.impl.io.task;

import com.emc.mongoose.model.api.io.task.DataIoTask;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.util.LoadType;

import java.io.IOException;

public class BasicDataIoTask<T extends DataItem>
extends BasicIoTask<T>
implements DataIoTask<T> {

	protected final long contentSize;
	protected volatile long countBytesDone;
	protected volatile DataItem currRange;
	protected volatile long currRangeSize;
	protected volatile long nextRangeOffset;
	protected volatile int currRangeIdx;
	protected volatile int currDataLayerIdx;
	protected volatile long respDataTimeStart;

	public BasicDataIoTask(final LoadType ioType, final T item, final String dstPath)
	throws IOException {
		super(ioType, item, dstPath);
		item.reset();
		//currDataLayerIdx = item.getCurrLayerIndex();
		switch(ioType) {
			case CREATE:
			case READ:
				// TODO partial read support
				try {
					contentSize = item.size();
				} catch(IOException e) {
					throw new IllegalStateException();
				}
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
	public void reset() {
		super.reset();
		currRange = null;
		countBytesDone = currRangeSize = nextRangeOffset = currRangeIdx = 0;
		respDataTimeStart = currDataLayerIdx = 0;
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
	public final void startDataResponse() {
		respDataTimeStart = System.nanoTime() / 1000;
	}

	@Override
	public final int getDataLatency() {
		if(respDataTimeStart > respTimeDone) {
			return (int) (respDataTimeStart - reqTimeDone);
		} else {
			return -1;
		}
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
