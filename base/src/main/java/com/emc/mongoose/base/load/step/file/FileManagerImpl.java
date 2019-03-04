package com.emc.mongoose.base.load.step.file;

import static com.emc.mongoose.base.Constants.KEY_STEP_ID;
import static com.github.akurilov.commons.system.DirectMemUtil.REUSABLE_BUFF_SIZE_MAX;
import static org.apache.logging.log4j.CloseableThreadContext.put;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.async.AsyncLogger;

public class FileManagerImpl implements FileManager {

	static final String LOG_CONFIG_STEP_ID_PTRN = "${ctx:" + KEY_STEP_ID + "}";

	@Override
	public final String logFileName(final String loggerName, final String testStepId) {
		try (final CloseableThreadContext.Instance logCtx = put(KEY_STEP_ID, testStepId)) {
			final Logger logger = LogManager.getLogger(loggerName);
			final Appender appender = ((AsyncLogger) logger).getAppenders().get("opTraceFile");
			final String filePtrn = ((RollingRandomAccessFileAppender) appender).getFilePattern();
			return filePtrn.contains(LOG_CONFIG_STEP_ID_PTRN)
							? filePtrn.replace(LOG_CONFIG_STEP_ID_PTRN, testStepId)
							: filePtrn;
		}
	}

	@Override
	public final String newTmpFileName() throws IOException {
		if (!Files.exists(TMP_DIR)) {
			Files.createDirectories(TMP_DIR);
		}
		return Paths.get(TMP_DIR.toString(), Long.toString(System.nanoTime())).toString();
	}

	@Override
	public final byte[] readFromFile(final String fileName, final long offset) throws IOException {
		try (final SeekableByteChannel fileChannel = Files.newByteChannel(Paths.get(fileName), READ_OPTIONS)) {
			fileChannel.position(offset);
			final long remainingSize = fileChannel.size() - offset;
			if (remainingSize <= 0) {
				throw new EOFException();
			}
			final int buffSize = (int) Math.min(REUSABLE_BUFF_SIZE_MAX, remainingSize);
			if (buffSize > 0) {
				final ByteBuffer bb = ByteBuffer.allocate(buffSize);
				int doneSize = 0;
				int n;
				while (doneSize < buffSize) {
					n = fileChannel.read(bb);
					if (n < 0) {
						// unexpected but possible: the file is shorter than was estimated before
						final byte[] buff = new byte[bb.position()];
						bb.rewind();
						bb.get(buff);
						return buff;
					} else {
						doneSize += n;
					}
				}
				return bb.array();
			} else {
				return EMPTY;
			}
		}
	}

	@Override
	public final void writeToFile(final String fileName, final byte[] buff) throws IOException {
		try (final ByteChannel fileChannel = Files.newByteChannel(Paths.get(fileName), APPEND_OPEN_OPTIONS)) {
			final ByteBuffer bb = ByteBuffer.wrap(buff);
			int n = 0;
			while (n < buff.length) {
				n = fileChannel.write(bb);
			}
		}
	}

	@Override
	public final long fileSize(final String fileName) {
		return new File(fileName).length();
	}

	@Override
	public final void truncateFile(final String fileName, final long size) throws IOException {
		try (final SeekableByteChannel fileChannel = Files.newByteChannel(Paths.get(fileName), WRITE_OPEN_OPTIONS)) {
			fileChannel.truncate(size);
		}
	}

	@Override
	public final void deleteFile(final String fileName) throws IOException {
		if (!new File(fileName).delete()) {
			throw new FileSystemException(fileName, null, "Failed to delete");
		}
	}
}
