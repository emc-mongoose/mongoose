package com.emc.mongoose.scenario.step.node;

import com.emc.mongoose.api.model.svc.ServiceBase;
import com.emc.mongoose.scenario.step.FileManagerService;
import com.emc.mongoose.scenario.step.FileService;
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.async.AsyncLogger;

import java.rmi.RemoteException;

import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;

public final class BasicFileManagerService
extends ServiceBase
implements FileManagerService {

	public BasicFileManagerService(final int port) {
		super(port);
	}

	@Override
	public String name() {
		return SVC_NAME;
	}

	@Override
	protected final void doStart() {
		super.doStart();
		Loggers.MSG.info("Service \"{}\" started @ port #{}", SVC_NAME, port);
	}

	@Override
	protected final void doClose() {
		Loggers.MSG.info("Service \"{}\" closed", SVC_NAME);
	}

	@Override
	public String createFileService(final String path)
	throws RemoteException {
		final FileService fileSvc = new BasicFileService(path, port);
		fileSvc.start();
		Loggers.MSG.info("New file service started @ port #{}: {}", port, fileSvc.name());
		return fileSvc.name();
	}

	@Override
	public String createLogFileService(final String loggerName, final String testStepId)
	throws RemoteException {
		try(
			final CloseableThreadContext.Instance
				logCtx = CloseableThreadContext.put(KEY_TEST_STEP_ID, testStepId)
		) {
			final Logger logger = LogManager.getLogger(loggerName);
			final Appender appender = ((AsyncLogger) logger).getAppenders().get("ioTraceFile");
			final String filePtrn = ((RollingRandomAccessFileAppender) appender).getFilePattern();
			final String fileName = filePtrn.contains("${ctx:stepId}") ?
				filePtrn.replace("${ctx:stepId}", testStepId) :
				filePtrn;
			return createFileService(fileName);
		}
	}
}
