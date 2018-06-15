package com.emc.mongoose.load.step;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.async.AsyncLogger;

import java.io.IOException;

import static com.emc.mongoose.Constants.KEY_STEP_ID;

public class StepFileManagerImpl
implements StepFileManager {

	public static final StepFileManagerImpl INSTANCE = new StepFileManagerImpl();

	@Override
	public String createFile(final String path)
	throws IOException {
		final StepFile file = new StepFileImpl(path);
		return file.filePath();
	}

	@Override
	public String createLogFile(final String loggerName, final String testStepId)
	throws IOException {
		try(
			final CloseableThreadContext.Instance
				logCtx = CloseableThreadContext.put(KEY_STEP_ID, testStepId)
		) {
			final Logger logger = LogManager.getLogger(loggerName);
			final Appender appender = ((AsyncLogger) logger).getAppenders().get("ioTraceFile");
			final String filePtrn = ((RollingRandomAccessFileAppender) appender).getFilePattern();
			final String fileName = filePtrn.contains("${ctx:stepId}") ?
				filePtrn.replace("${ctx:stepId}", testStepId) :
				filePtrn;
			return createFile(fileName);
		}
	}
}
