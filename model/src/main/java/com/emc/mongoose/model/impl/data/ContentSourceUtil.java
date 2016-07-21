package com.emc.mongoose.model.impl.data;

import com.emc.mongoose.model.api.data.ContentSource;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 Created by kurila on 31.05.16.
 */
public class ContentSourceUtil {

	public static ContentSource clone(ContentSource anotherContentSrc) {
		if(anotherContentSrc instanceof FileContentSource) {
			return new FileContentSource((FileContentSource) anotherContentSrc);
		} else if(anotherContentSrc instanceof SeedContentSource) {
			return new SeedContentSource((SeedContentSource) anotherContentSrc);
		} else {
			throw new IllegalStateException("Unhandled content source type");
		}
	}

	public static ContentSource getInstance(
		final String contentFilePath, final String seed, final long ringSize
	) throws IOException, IllegalStateException {
		final ContentSource instance;
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
			instance = new SeedContentSource(Long.parseLong(seed, 0x10), ringSize);
		}
		return instance;
	}
}
