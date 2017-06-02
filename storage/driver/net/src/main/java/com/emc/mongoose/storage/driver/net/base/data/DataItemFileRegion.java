package com.emc.mongoose.storage.driver.net.base.data;

import com.emc.mongoose.model.item.DataItem;

import io.netty.channel.FileRegion;
import io.netty.util.AbstractReferenceCounted;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class DataItemFileRegion
extends AbstractReferenceCounted
implements FileRegion {
	
	protected final DataItem dataItem;
	protected final long baseItemSize;
	protected long doneByteCount = 0;

	public DataItemFileRegion(final DataItem dataItem)
	throws IOException {
		this.dataItem = dataItem;
		this.baseItemSize = dataItem.size();
	}

	@Override
	public long position() {
		return doneByteCount;
	}

	@Deprecated
	@Override
	public long transfered() {
		return doneByteCount;
	}

	@Override
	public long transferred() {
		return doneByteCount;
	}

	@Override
	public long count() {
		return baseItemSize;
	}

	@Override
	public long transferTo(final WritableByteChannel target, final long position)
	throws IOException {
		dataItem.position(position);
		doneByteCount += dataItem.write(target, baseItemSize - position);
		return doneByteCount;
	}

	@Override
	public FileRegion retain() {
		super.retain();
		return this;
	}

	@Override
	public FileRegion retain(int increment) {
		super.retain(increment);
		return this;
	}
	
	@Override
	public FileRegion touch() {
		return touch(this);
	}

	@Override
	public FileRegion touch(Object hint) {
		return this;
	}

	@Override
	protected void deallocate() {
	}
}
