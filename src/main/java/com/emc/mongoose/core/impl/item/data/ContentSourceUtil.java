package com.emc.mongoose.core.impl.item.data;

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.core.api.item.data.ContentSource;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 Created by kurila on 31.05.16.
 */
public class ContentSourceUtil {

	private final static Logger LOG = LogManager.getLogger();
	private final static Lock LOCK = new ReentrantLock();

	public static ContentSource DEFAULT = null;

	public static ContentSource getDefaultInstance()
	throws IllegalStateException {
		LOCK.lock();
		try {
			if(DEFAULT == null) {
				final AppConfig appConfig = BasicConfig.THREAD_CONTEXT.get();
				DEFAULT = getInstance(appConfig);
			}
		} catch(final Throwable e) {
			LogUtil.exception(LOG, Level.FATAL, e, "Failed to init the ring buffer");
		} finally {
			LOCK.unlock();
		}
		return DEFAULT;
	}

	public static ContentSource getInstance(final AppConfig appConfig)
	throws IOException, IllegalStateException {
		final ContentSource instance;
		final String contentFilePath = appConfig.getItemDataContentFile();
		if(contentFilePath != null && !contentFilePath.isEmpty()) {
			final Path p = Paths.get(contentFilePath);
			if(Files.exists(p) && !Files.isDirectory(p) &&
				Files.isReadable(p)) {
				final File f = p.toFile();
				final long fileSize = f.length();
				if(fileSize > 0) {
					try(
						final ReadableByteChannel rbc = Files
							.newByteChannel(p, StandardOpenOption.READ)
					) {
						instance = new FileContentSource(rbc, fileSize);
					}
				} else {
					throw new IllegalStateException(
						"Content source file @" + contentFilePath + " is empty"
					);
				}
			} else {
				throw new IllegalStateException(
					"Content source file @" + contentFilePath + " doesn't exist/" +
					"not readable/is a directory"
				);
			}
		} else {
			instance = new SeedContentSource(appConfig);
		}
		return instance;
	}

	public static ContentSource clone(ContentSource anotherContentSrc) {
		if(anotherContentSrc instanceof FileContentSource) {
			return new FileContentSource((FileContentSource) anotherContentSrc);
		} else if(anotherContentSrc instanceof SeedContentSource) {
			return new SeedContentSource((SeedContentSource) anotherContentSrc);
		} else {
			throw new IllegalStateException("Unhandled content source type");
		}
	}
}
