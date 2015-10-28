package com.emc.mongoose.core.impl.data.content;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.data.content.ContentSource;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
		this(RunTimeConfig.getContext().getDataContentFPath());
	}
	//
	public FileContentSource(final String fPath)
	throws IOException, InvalidPathException {
		this(Paths.get(fPath));
	}
	//
	private FileContentSource(final Path fPath)
	throws IOException {
		this(Files.newByteChannel(fPath, StandardOpenOption.READ), Files.size(fPath));
	}
	//
	private FileContentSource(final ReadableByteChannel contentSrcChan, final long size)
	throws IOException {
		super(contentSrcChan, size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size);
	}
	//

}
