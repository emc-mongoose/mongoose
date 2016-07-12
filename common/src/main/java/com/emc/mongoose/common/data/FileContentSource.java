package com.emc.mongoose.common.data;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.InvalidPathException;
/**
 Created by kurila on 16.10.15.
 */
public final class FileContentSource
extends ContentSourceBase
implements ContentSource {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public FileContentSource()
	throws IOException, InvalidPathException {
		super();
	}
	//
	public FileContentSource(final ReadableByteChannel contentSrcChan, final long size)
	throws IOException {
		super(contentSrcChan, size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size);
	}
	//
	public FileContentSource(final FileContentSource anotherContentSource) {
		super(anotherContentSource);
	}
}
