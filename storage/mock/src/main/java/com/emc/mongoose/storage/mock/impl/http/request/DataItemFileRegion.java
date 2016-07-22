package com.emc.mongoose.storage.mock.impl.http.request;

import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import io.netty.channel.FileRegion;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;

import java.io.File;
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
        return dataSize;
    }

    @Override
    public long transferTo(WritableByteChannel target, long position)
            throws IOException {
        dataObject.setRelativeOffset(position);
        doneByteCount += dataObject.write(target, dataSize - position);
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
