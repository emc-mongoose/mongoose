package com.emc.mongoose.load.step;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public interface FileManager {

	byte[] EMPTY = new byte[0];
	OpenOption[] READ_OPTIONS = new OpenOption[] {
		StandardOpenOption.READ,
		LinkOption.NOFOLLOW_LINKS
	};
	OpenOption[] WRITE_OPEN_OPTIONS = new OpenOption[] {
		StandardOpenOption.CREATE,
		StandardOpenOption.WRITE,
		StandardOpenOption.TRUNCATE_EXISTING
	};
	OpenOption[] APPEND_OPEN_OPTIONS = new OpenOption[] {
		StandardOpenOption.CREATE,
		StandardOpenOption.WRITE,
		StandardOpenOption.APPEND
	};
	Path TMP_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "mongoose");

	/**
	 * Generate the temporary file name
	 * @return the temporary file name
	 */
	String newTmpFileName()
	throws IOException;

	/**
	 Read the next batch of bytes from the remote file
	 @return the bytes been read, may return empty buffer in case of EOF
	 @throws IllegalStateException if the file isn't open yet either
	 EOFException if end of file is reached
	 */
	byte[] readFromFile(final String fileName, final long offset)
	throws IOException;

	/**
	 Write some bytes to the remote file. Blocks until all the bytes are written.
	 @param buff the bytes to write to the remote file
	 another IOException
	 */
	void writeToFile(final String fileName, final byte[] buff)
	throws IOException;

	long fileSize(final String fileName)
	throws IOException;

	void truncateFile(final String fileName, final long size)
	throws IOException;

	void deleteFile(final String fileName)
	throws IOException;
}
