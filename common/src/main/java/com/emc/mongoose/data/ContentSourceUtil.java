package com.emc.mongoose.data;

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
