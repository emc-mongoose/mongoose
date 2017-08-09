package com.emc.mongoose.api.model.data;

import com.emc.mongoose.api.common.SizeInBytes;
import com.emc.mongoose.api.common.math.MathUtil;

import java.io.Closeable;
import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.READ;

/**
 Created by kurila on 29.09.14.
 A finite data input for data generation purposes.
 */
public interface DataInput
extends Closeable, Externalizable {

	enum Type {
		FILE, SEED
	}

	int getSize();

	ByteBuffer getLayer(final int layerIndex);

	static DataInput getInstance(
		final String inputFilePath, final String seed, final SizeInBytes layerSize,
		final int layerCacheLimit
	) throws IOException, IllegalStateException, IllegalArgumentException {
		final DataInput instance;
		final long layerSizeBytes = layerSize.get();
		if(layerSizeBytes > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Item data layer size should be less than 2GB");
		}
		if(inputFilePath != null && !inputFilePath.isEmpty()) {
			final Path p = Paths.get(inputFilePath);
			if(Files.exists(p) && !Files.isDirectory(p) &&
				Files.isReadable(p)) {
				final File f = p.toFile();
				final long fileSize = f.length();
				if(fileSize > 0) {
					if(fileSize > Integer.MAX_VALUE) {
						throw new AssertionError(
							"Item data input file size should be less than 2GB"
						);
					}
					try(final ReadableByteChannel rbc = Files.newByteChannel(p, READ)) {
						instance = new ExternalDataInput(rbc, (int) fileSize, layerCacheLimit);
					}
				} else {
					throw new AssertionError(
						"Item data input file @" + p.toAbsolutePath() + " is empty"
					);
				}
			} else {
				throw new AssertionError(
					"Item data input file @" + p.toAbsolutePath() + " doesn't exist/" +
						"not readable/is a directory"
				);
			}
		} else {
			instance = new SeedDataInput(
				Long.parseLong(seed, 0x10), layerSizeBytes, layerCacheLimit
			);
		}
		return instance;
	}

	static void generateData(final ByteBuffer byteLayer, final long seed) {
		final int
			ringBuffSize = byteLayer.capacity(),
			countWordBytes = Long.SIZE / Byte.SIZE,
			countWords = ringBuffSize / countWordBytes,
			countTailBytes = ringBuffSize % countWordBytes;
		long word = seed;
		int i;
		// 64-bit words
		byteLayer.clear();
		for(i = 0; i < countWords; i ++) {
			byteLayer.putLong(word);
			word = MathUtil.xorShift(word);
		}
		// tail bytes
		final ByteBuffer tailBytes = ByteBuffer.allocateDirect(countWordBytes);
		tailBytes.asLongBuffer().put(word).rewind();
		for(i = 0; i < countTailBytes; i ++) {
			byteLayer.put(countWordBytes * countWords + i, tailBytes.get(i));
		}
	}
}
