package com.emc.mongoose.model.impl.data;
//
import com.emc.mongoose.model.api.data.ContentSource;
//
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.InvalidPathException;
/**
 Created by kurila on 16.10.15.
 */
public final class FileContentSource
extends ContentSourceBase
implements ContentSource {
	
	public FileContentSource()
	throws IOException, InvalidPathException {
		super();
	}
	
	public FileContentSource(final ReadableByteChannel contentSrcChan, final long size)
	throws IOException {
		super(contentSrcChan, size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size);
	}
	
	public FileContentSource(final FileContentSource anotherContentSource) {
		super(anotherContentSource);
	}
}
