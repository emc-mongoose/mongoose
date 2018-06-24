package com.emc.mongoose.load.step;

import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import static org.apache.logging.log4j.CloseableThreadContext.put;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.async.AsyncLogger;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static com.emc.mongoose.Constants.KEY_STEP_ID;

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
	 Determine the file name for the given logger and step id pair
	 @param loggerName
	 @param testStepId
	 @return the file name
	 */
	String logFileName(final String loggerName, final String testStepId);

	/**
	 * Generate the temporary file name
	 * @return the temporary file name
	 */
	String newTmpFileName()
	throws IOException;

	/**
	 Read the next batch of bytes from the remote file
	 @return The bytes been read, may return empty buffer in case of EOF
	 @throws EOFException if end of file is reached
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
