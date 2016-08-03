package com.emc.mongoose.storage.driver.http.base.data;

import com.emc.mongoose.model.api.item.DataItem;
import io.netty.channel.FileRegion;
import io.netty.util.AbstractReferenceCounted;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class DataItemFileRegion<T extends DataItem>
extends AbstractReferenceCounted
implements FileRegion {
	
	protected final T dataObject;
	protected final long baseItemSize;
	protected long doneByteCount = 0;

	public DataItemFileRegion(final T dataObject)
	throws IOException {
		this.dataObject = dataObject;
		this.baseItemSize = dataObject.size();
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
		dataObject.position(position);
		doneByteCount += dataObject.write(target, baseItemSize - position);
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
