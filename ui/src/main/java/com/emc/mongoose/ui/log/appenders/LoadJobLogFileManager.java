package com.emc.mongoose.ui.log.appenders;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.ConfigurationFactoryData;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.config.Configuration;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static java.io.File.separatorChar;

/** Created by andrey on 13.03.15. */
public final class LoadJobLogFileManager
extends AbstractManager {
	//
	public static final List<LoadJobLogFileManager> INSTANCES = new ArrayList<>();
	//
	private final String fileName, uriAdvertise;
	private final boolean flagAppend, flagLock, flagBuffered;
	private final int buffSize;
	private final Map<String, OutputStream> outStreamsMap = new HashMap<>();
	private final Layout<? extends Serializable> layout;
	//
	protected LoadJobLogFileManager(
		final LoggerContext loggerContext, final String fileName, final boolean flagAppend,
		final boolean flagLock, final boolean flagBuffered, final String uriAdvertise,
		final Layout<? extends Serializable> layout, final int buffSize
	) {
		super(loggerContext, fileName);
		this.fileName = fileName;
		this.flagAppend = flagAppend;
		this.flagLock = flagLock;
		this.flagBuffered = flagBuffered;
		this.uriAdvertise = uriAdvertise;
		this.layout = layout;
		this.buffSize = buffSize;
		INSTANCES.add(this);
	}

	/** Factory Data */
	private static class FactoryData
	extends ConfigurationFactoryData {
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
			final Layout<? extends Serializable> layout, final Configuration config
		) {
			super(config);
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
	private static final class LoadJobFileManagerFactory
	implements ManagerFactory<LoadJobLogFileManager, FactoryData> {
		/**
		 * Create a FileManager.
		 * @param fileName The prefix for the name of the File.
		 * @param data The FactoryData
		 * @return The FileManager for the File.
		 */
		@Override
		public LoadJobLogFileManager createManager(final String fileName, final FactoryData data) {
			return new LoadJobLogFileManager(
				data.getLoggerContext(), fileName, data.flagAppend, data.flagLock,
				data.flagBuffered, data.uriAdvertise, data.layout, data.buffSize
			);
		}
	}
	//
	private static final LoadJobFileManagerFactory FACTORY = new LoadJobFileManagerFactory();
	//
	public static LoadJobLogFileManager getRunIdFileManager(
		final String fileName,
		final boolean flagAppend, final boolean flagLock, final boolean flagBuffered,
		final String uriAdvertise, final Layout<? extends Serializable> layout, final int buffSize,
		final Configuration config
	) {
		return LoadJobLogFileManager.class.cast(
			getManager(
				fileName, FACTORY,
				new FactoryData(
					flagAppend, flagLock, flagBuffered, buffSize, uriAdvertise, layout, config
				)
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
	protected final void write(
		final String jobName, final byte[] buff, final int offset, final int len
	) {
		final OutputStream outStream = getOutputStream(jobName);
		try {
			outStream.write(buff, offset, len);
		} catch (final Throwable e) {
			throw new AppenderLoggingException(
				"Failed to write to the stream \"" + getName() + "\" for job name \""+jobName+"\"",
				e
			);
		}
	}
	//
	protected final void write(final String jobName, final byte[] bytes)  {
		write(jobName, bytes, 0, bytes.length);
	}
	//
	protected final OutputStream prepareNewFile(final String jobName) {
		OutputStream newOutPutStream = null;
		final File
			outPutFile = new File(
				jobName == null ?
					getBaseDir() + separatorChar + "log" + separatorChar + fileName :
					getBaseDir() + separatorChar + "log" + separatorChar + jobName + separatorChar + fileName
			),
			parentFile = outPutFile.getParentFile();
		final boolean existedBefore = outPutFile.exists();
		if(!existedBefore && parentFile != null && !parentFile.exists()) {
			parentFile.mkdirs();
		}
		//
		try {
			newOutPutStream = new BufferedOutputStream(
				new FileOutputStream(outPutFile.getPath(), flagAppend), this.buffSize
			);
			outStreamsMap.put(jobName, newOutPutStream);
			if(layout != null && (!flagAppend || !existedBefore)) {
				final byte header[] = layout.getHeader();
				if(header != null) {
					newOutPutStream.write(layout.getHeader());
				}
			}
		} catch(final IOException e) {
			e.printStackTrace(System.err);
		}
		//
		return newOutPutStream;
	}
	//
	protected final OutputStream getOutputStream(final String currRunId) {
		OutputStream currentOutPutStream = outStreamsMap.get(currRunId);
		if(currentOutPutStream == null) {
			currentOutPutStream = prepareNewFile(currRunId);
		}
		return currentOutPutStream;
	}
	//
	@Override
	public final void close() {
		for(final OutputStream outStream : outStreamsMap.values()) {
			try {
				if(layout != null) {
					final byte[] footer = layout.getFooter();
					if(footer != null) {
						outStream.write(footer);
					}
				}
				outStream.flush();
				outStream.close();
			} catch(final IOException e) {
				e.printStackTrace(System.err);
			}
		}
		outStreamsMap.clear();
		INSTANCES.remove(this);
	}
	//
	public static void closeAll(final String runId) {
		final LoadJobLogFileManager[] managers = new LoadJobLogFileManager[INSTANCES.size()];
		INSTANCES.toArray(managers);
		for(final LoadJobLogFileManager manager : managers) {
			if(manager.outStreamsMap.containsKey(runId)) {
				manager.close();
			}
		}
	}
	/** Flushes all available output streams */
	public final void flush() {
		try {
			for(final OutputStream outStream : outStreamsMap.values()) {
				try {
					outStream.flush();
				} catch(final IOException e) {
					e.printStackTrace(System.err);
				}
			}
		} catch(final ConcurrentModificationException e) {
			e.printStackTrace(System.err);
		}
	}
	//
	public static void flush(final String runId) {
		for(final LoadJobLogFileManager instance : INSTANCES) {
			final OutputStream outStream = instance.outStreamsMap.get(runId);
			if(outStream != null) {
				try {
					outStream.flush();
				} catch(final IOException e) {
					e.printStackTrace(System.err);
				}
			}
		}
	}
	//
	public static void flushAll()
	throws IOException {
		for(final LoadJobLogFileManager manager : INSTANCES) {
			manager.flush();
		}
	}
}
