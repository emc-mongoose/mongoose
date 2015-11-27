package com.emc.mongoose.storage.mock.impl.web.request;

import com.emc.mongoose.core.api.data.DataItem;
import io.netty.channel.FileRegion;
import io.netty.util.AbstractReferenceCounted;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class DataItemFileRegion
		extends AbstractReferenceCounted
		implements FileRegion {

	private final DataItem dataItem;
	private final long dataSize;

	private long doneByteCount = 0;

	public DataItemFileRegion(final DataItem dataItem) {
		this.dataItem = dataItem;
		this.dataSize = dataItem.getSize();
	}

	@Override
	protected void deallocate() {
	}

	@Override
	public long position() {
		return doneByteCount;
	}

	@Override
	public long transfered() {
		return doneByteCount;
	}

	@Override
	public long count() {
		return dataSize;
	}

	@Override
	public long transferTo(WritableByteChannel target, long position)
			throws IOException {
		dataItem.setRelativeOffset(position);
		long n = dataItem.write(target, dataSize - position);
		doneByteCount += n;
		return n;
	}
}
