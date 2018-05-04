package com.emc.mongoose.scenario.step.node;

import com.emc.mongoose.model.svc.ServiceBase;
import com.emc.mongoose.scenario.step.FileManagerService;
import com.emc.mongoose.scenario.step.FileService;
import com.emc.mongoose.logging.Loggers;
import static com.emc.mongoose.model.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.model.Constants.KEY_TEST_STEP_ID;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.async.AsyncLogger;

import java.rmi.RemoteException;

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
		try(
			final CloseableThreadContext.Instance logCtx = CloseableThreadContext
				.put(KEY_CLASS_NAME, BasicFileManagerService.class.getSimpleName())
		) {
			super.doStart();
			Loggers.MSG.info("Service \"{}\" started @ port #{}", SVC_NAME, port);
		}
	}

	@Override
	protected final void doClose() {
		try(
			final CloseableThreadContext.Instance logCtx = CloseableThreadContext
				.put(KEY_CLASS_NAME, BasicFileManagerService.class.getSimpleName())
		) {
			Loggers.MSG.info("Service \"{}\" closed", SVC_NAME);
		}
	}

	@Override
	public String createFileService(final String path)
	throws RemoteException {
		final FileService fileSvc = new BasicFileService(path, port);
		try(
			final CloseableThreadContext.Instance logCtx = CloseableThreadContext
				.put(KEY_CLASS_NAME, BasicFileManagerService.class.getSimpleName())
		) {
			fileSvc.start();
			Loggers.MSG.info("New file service started @ port #{}: {}", port, fileSvc.name());
		}
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
