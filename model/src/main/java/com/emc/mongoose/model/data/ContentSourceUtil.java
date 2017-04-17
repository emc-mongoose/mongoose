package com.emc.mongoose.model.data;

import com.emc.mongoose.common.api.SizeInBytes;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.READ;

/**
 Created by kurila on 31.05.16.
 */
public class ContentSourceUtil {

	public static ContentSource getInstance(
		final String contentFilePath, final String seed, final SizeInBytes ringSize,
		final int cacheLimit
	) throws IOException, IllegalStateException, IllegalArgumentException {
		final ContentSource instance;
		final long ringSizeBytes = ringSize.get();
		if(ringSizeBytes > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Ring buffer size should be less than 2GB");
		}
		if(contentFilePath != null && !contentFilePath.isEmpty()) {
			final Path p = Paths.get(contentFilePath);
			if(Files.exists(p) && !Files.isDirectory(p) &&
				Files.isReadable(p)) {
				final File f = p.toFile();
				final long fileSize = f.length();
				if(fileSize > 0) {
					try(final ReadableByteChannel rbc = Files.newByteChannel(p, READ)) {
						instance = new BasicContentSource(
							rbc, (int) (fileSize > ringSizeBytes ? ringSizeBytes : fileSize),
							cacheLimit
						);
					}
				} else {
					throw new AssertionError(
						"Content source file @" + p.toAbsolutePath() + " is empty"
					);
				}
			} else {
				throw new AssertionError(
					"Content source file @" + p.toAbsolutePath() + " doesn't exist/" +
						"not readable/is a directory"
				);
			}
		} else {
			instance = new SeedContentSource(Long.parseLong(seed, 0x10), ringSizeBytes, cacheLimit);
		}
		return instance;
	}
}
