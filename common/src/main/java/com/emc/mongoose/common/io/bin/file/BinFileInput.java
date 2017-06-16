package com.emc.mongoose.common.io.bin.file;

import com.emc.mongoose.common.io.bin.BinInput;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 An item input implementation deserializing something from the specified file.
 */
public class BinFileInput<T>
extends BinInput<T>
implements FileItemInput<T> {
	
	protected final Path srcPath;

	/**
	 @param srcPath the path to the file which should be used to restore the serialized items
	 @throws IOException if unable to open the file for reading
	 */
	public BinFileInput(final Path srcPath)
	throws IOException {
		super(buildObjectInputStream(srcPath));
		this.srcPath = srcPath;
	}
	
	protected static ObjectInputStream buildObjectInputStream(final Path itemsSrcPath)
	throws IOException {
		return new ObjectInputStream(
			new BufferedInputStream(Files.newInputStream(itemsSrcPath, StandardOpenOption.READ))
		);
	}
	
	@Override
	public String toString() {
		return "binFileInput<" + srcPath.getFileName() + ">";
	}
	
	@Override
	public final Path getFilePath() {
		return srcPath;
	}
	
	@Override
	public void reset()
	throws IOException {
		if(itemsSrc != null) {
			itemsSrc.close();
		}
		setItemsSrc(buildObjectInputStream(srcPath));
	}
}
