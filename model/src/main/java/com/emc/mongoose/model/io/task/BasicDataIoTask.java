package com.emc.mongoose.model.io.task;

import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.data.DataRangesConfig;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.io.IoType;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class BasicDataIoTask<T extends DataItem>
extends BasicIoTask<T>
implements DataIoTask<T> {

	protected long contentSize;
	protected long itemDataOffset;

	protected transient volatile ContentSource contentSrc;
	protected transient volatile long countBytesDone;
	protected transient volatile long respDataTimeStart;
	
	public BasicDataIoTask() {
		super();
	}
	
	public BasicDataIoTask(
		final IoType ioType, final T item, final String srcPath, final String dstPath,
		final DataRangesConfig rangesConfig
	) {
		super(ioType, item, srcPath, dstPath);
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
	public final long getRespDataTimeStart() {
		return respDataTimeStart;
	}

	@Override
	public final void startDataResponse() {
		respDataTimeStart = System.nanoTime() / 1000;
	}

	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeLong(contentSize);
	}
	
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		itemDataOffset = item.offset();
		contentSrc = item.getContentSrc();
		contentSize = in.readLong();
	}
}
