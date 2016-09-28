package com.emc.mongoose.model.impl.io.task;

import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.api.data.DataRangesConfig;
import com.emc.mongoose.model.api.io.task.DataIoTask;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.util.LoadType;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class BasicDataIoTask<T extends DataItem>
extends BasicIoTask<T>
implements DataIoTask<T> {

	protected long contentSize;
	protected long itemDataOffset;
	protected volatile ContentSource contentSrc;
	
	protected volatile long countBytesDone;
	protected volatile long respDataTimeStart;
	
	public BasicDataIoTask() {
		super();
	}
	
	public BasicDataIoTask(
		final LoadType ioType, final T item, final String dstPath,
		final DataRangesConfig rangesConfig
	) {
		super(ioType, item, dstPath);
		item.reset();
		//currDataLayerIdx = item.getCurrLayerIndex();
		switch(ioType) {
			case CREATE:
			case READ:
				// TODO partial read support, use rangesConfig
				try {
					contentSize = item.size();
				} catch(IOException e) {
					throw new IllegalStateException();
				}
				break;
			default:
				contentSize = 0;
				break;
		}
		itemDataOffset = item.offset();
		contentSrc = item.getContentSrc();
	}
	
	@Override
	public void reset() {
		super.reset();
		countBytesDone = 0;
		respDataTimeStart = 0;
	}

	@Override
	public final long getCountBytesDone() {
		return countBytesDone;
	}

	@Override
	public final void setCountBytesDone(final long n) {
		this.countBytesDone = n;
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
	
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeLong(contentSize);
		out.writeLong(countBytesDone);
		out.writeLong(respDataTimeStart);
	}
	
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		itemDataOffset = item.offset();
		contentSrc = item.getContentSrc();
		contentSize = in.readLong();
		countBytesDone = in.readLong();
		respDataTimeStart = in.readLong();
	}
}
