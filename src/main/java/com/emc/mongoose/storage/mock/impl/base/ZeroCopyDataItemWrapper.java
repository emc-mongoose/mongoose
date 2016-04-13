package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.core.api.item.data.DataItem;

import io.netty.channel.FileRegion;
import io.netty.util.AbstractReferenceCounted;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class ZeroCopyDataItemWrapper<T extends DataItem>
extends AbstractReferenceCounted
implements FileRegion {

	protected final T object;
	protected final long size;
	protected long doneByteCount = 0;

	public ZeroCopyDataItemWrapper(final T object) {
		this.object = object;
		this.size = object.getSize();
	}

	@Override
	protected final void deallocate() {
	}

	@Override
	public final long position() {
		return doneByteCount;
	}

	@Override
	public final long transfered() {
		return doneByteCount;
	}

	@Override
	public final long count() {
		return size;
	}

	@Override
	public long transferTo(final WritableByteChannel tgtChan, final long position)
	throws IOException {
		object.setRelativeOffset(position);
		doneByteCount += object.write(tgtChan, size - position);
		return doneByteCount;
	}
}
