package com.emc.mongoose.common.io.bin.file;

import com.emc.mongoose.common.io.bin.BinOutput;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 An item input implementation serializing something to the specified file.
 */
public class BinFileOutput<T>
extends BinOutput<T>
implements FileItemOutput<T> {
	
	protected final Path dstPath;

	/**
	 @param dstPath the path to the file which should be used to store the serialized items
	 @throws IOException if unable to open the file for writing
	 */
	public BinFileOutput(final Path dstPath)
	throws IOException {
		super(
			new ObjectOutputStream(
				new BufferedOutputStream(
					Files.newOutputStream(
						dstPath, StandardOpenOption.APPEND, StandardOpenOption.WRITE
					)
				)
			)
		);
		this.dstPath = dstPath;
	}
	
	public BinFileOutput()
	throws IOException {
		this(Files.createTempFile(null, ".bin"));
		this.dstPath.toFile().deleteOnExit();
	}
	
	@Override
	public BinFileInput<T> getInput()
	throws IOException {
		return new BinFileInput<>(dstPath);
	}
	
	@Override
	public String toString() {
		return "binFileOutput<" + dstPath.getFileName() + ">";
	}
	
	@Override
	public final Path getFilePath() {
		return dstPath;
	}
}
