package com.emc.mongoose.core.impl.util.log;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.status.StatusLogger;
//
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
/** Created by andrey on 13.03.15. */
public final class RunIdFileManager
extends AbstractManager {
	//
	private final static Logger LOG = StatusLogger.getLogger();
	private final static String PATH_LOG_DIR = System.getProperty("dir.root") + File.separator + "log";
	//
	private final String fileName, uriAdvertise;
	private final boolean flagAppend, flagLock, flagBuffered;
	private final int buffSize;
	private final Map<String, FileOutputStream> outStreamsMap = new HashMap<>();
	private final Layout<? extends Serializable> layout;
	//
	protected RunIdFileManager(
		final String fileName, final boolean flagAppend, final boolean flagLock, final boolean flagBuffered,
		final String uriAdvertise, final Layout<? extends Serializable> layout, final int buffSize
	) {
		super(fileName);
		this.fileName = fileName;
		this.flagAppend = flagAppend;
		this.flagLock = flagLock;
		this.flagBuffered = flagBuffered;
		this.uriAdvertise = uriAdvertise;
		this.layout = layout;
		this.buffSize = buffSize;
	}
	/** Factory Data */
	private static class FactoryData {
		//
		private final boolean flagAppend;
		private final boolean flagLock;
		private final boolean flagBuffered;
		private final int buffSize;
		private final String uriAdvertise;
		private final Layout<? extends Serializable> layout;
		/**
		 * Constructor.
		 * @param flagAppend Append status.
		 * @param flagLock Locking status.
		 * @param flagBuffered Buffering flag.
		 * @param buffSize Buffer size.
		 * @param uriAdvertise the URI to use when advertising the file
		 */
		public FactoryData(
			final boolean flagAppend, final boolean flagLock, final boolean flagBuffered,
			final int buffSize, final String uriAdvertise,
			final Layout<? extends Serializable> layout
		) {
			this.flagAppend = flagAppend;
			this.flagLock = flagLock;
			this.flagBuffered = flagBuffered;
			this.buffSize = buffSize;
			this.uriAdvertise = uriAdvertise;
			this.layout = layout;
		}
	}
	/**
	 * Factory to create a FileManager.
	 */
	private final static class RunIdFileManagerFactory
	implements ManagerFactory<RunIdFileManager, FactoryData> {
		/**
		 * Create a FileManager.
		 * @param fileName The prefix for the name of the File.
		 * @param data The FactoryData
		 * @return The FileManager for the File.
		 */
		@Override
		public RunIdFileManager createManager(final String fileName, final FactoryData data) {
			return new RunIdFileManager(
				fileName, data.flagAppend, data.flagLock, data.flagBuffered,
				data.uriAdvertise, data.layout, data.buffSize
			);
		}
	}
	//
	private final static RunIdFileManagerFactory FACTORY = new RunIdFileManagerFactory();
	//
	public static RunIdFileManager getRunIdFileManager(
		final String fileName,
		final boolean flagAppend, final boolean flagLock, final boolean flagBuffered,
		final String uriAdvertise, final Layout<? extends Serializable> layout, final int buffSize
	) {
		return RunIdFileManager.class.cast(
			getManager(
				fileName, FACTORY,
				new FactoryData(flagAppend, flagLock, flagBuffered, buffSize, uriAdvertise, layout)
			)
		);
	}
	//
	public final String getFileName() {
		return fileName;
	}
	//
	public final boolean isAppend() {
		return flagAppend;
	}
	//
	public final boolean isLocking() {
		return flagLock;
	}
	//
	public final int getBufferSize() {
		return buffSize;
	}
	//
	protected final synchronized void write(
		final String runMode, final String currRunId,
		final byte[] buff, final int offset, final int len
	) {
		final FileOutputStream outStream = getOutputStream(runMode, currRunId);
		if(flagLock) {
			final FileChannel channel = outStream.getChannel();
			try {
				/* Lock the whole file. This could be optimized to only lock from the current file
				position. Note that locking may be advisory on some systems and mandatory on others,
				so locking just from the current position would allow reading on systems where
				locking is mandatory.  Also, Java 6 will throw an exception if the region of the
				file is already locked by another FileChannel in the same JVM. Hopefully, that will
				be avoided since every file should have a single file manager - unless two different
				files strings are configured that somehow map to the same file.*/
				final FileLock lock = channel.lock(0, Long.MAX_VALUE, false);
				try {
					outStream.write(buff, offset, len);
				} finally {
					lock.release();
				}
			} catch(final IOException e) {
				throw new AppenderLoggingException("Unable to obtain lock on " + getName(), e);
			}
		} else {
			try {
				outStream.write(buff, offset, len);
			} catch (final IOException e) {
				throw new AppenderLoggingException("Error writing to stream " + getName(), e);
			}
		}
	}
	//
	protected final void write(final String runMode, final String currRunId, final byte[] bytes)  {
		write(runMode, currRunId, bytes, 0, bytes.length);
	}
	//
	private final String
		FMT_FILE_PATH = "%s" + File.separator + "%s" + File.separator +"%s" + File.separator +"%s",
		FMT_FILE_PATH_NO_RUN_ID = "%s" + File.separator + File.separator +"%s";
	//
	protected final FileOutputStream getOutputStream(final String runMode, final String currRunId) {
		FileOutputStream currentOutPutStream = null;
		if(outStreamsMap.containsKey(currRunId)) {
			currentOutPutStream = outStreamsMap.get(currRunId);
		} else {
			final File
				outPutFile = new File(
					currRunId == null ?
						String.format(FMT_FILE_PATH_NO_RUN_ID, PATH_LOG_DIR, fileName) :
						String.format(FMT_FILE_PATH, PATH_LOG_DIR, runMode, currRunId, fileName)
				),
				parentFile = outPutFile.getParentFile();
			if(parentFile != null && !parentFile.exists()) {
				parentFile.mkdirs();
			}
			try {
				currentOutPutStream = new FileOutputStream(
					outPutFile.getPath(), flagAppend
				);
				outStreamsMap.put(currRunId, currentOutPutStream);
			} catch(final FileNotFoundException e) {
				TraceLogger.failure(
					LOG, Level.ERROR, e, "Failed to open log file for writing"
				);
			}
		}
		return currentOutPutStream;
	}
	//
	protected final synchronized void close() {
		for(final FileOutputStream outStream : outStreamsMap.values()) {
			try {
				outStream.close();
			} catch(final IOException e) {
				TraceLogger.failure(LOG, Level.WARN, e, "Failed to close the log file");
			}
		}
	}
	/** Flushes all available output streams */
	public final synchronized void flush() {
		for(final FileOutputStream outStream : outStreamsMap.values()) {
			try {
				outStream.flush();
			} catch(final IOException e) {
				TraceLogger.failure(LOG, Level.WARN, e, "Failed to flush the log");
			}
		}
	}
}
