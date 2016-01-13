package com.emc.mongoose.storage.mock.impl.web.request;

import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import io.netty.channel.FileRegion;
import io.netty.util.AbstractReferenceCounted;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class DataItemFileRegion<T extends MutableDataItemMock>
		extends AbstractReferenceCounted
		implements FileRegion {


	protected final T dataObject;
	protected final long dataSize;
	protected long doneByteCount = 0;

	public DataItemFileRegion(final T dataObject) {
		this.dataObject = dataObject;
		this.dataSize = dataObject.getSize();
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
		dataObject.setRelativeOffset(position);
		doneByteCount += dataObject.write(target, dataSize - position);
		return doneByteCount;
	}
}
