package com.emc.mongoose.node;

import com.emc.mongoose.api.model.svc.ServiceBase;
import com.emc.mongoose.scenario.sna.FileManagerService;
import com.emc.mongoose.scenario.sna.FileService;
import com.emc.mongoose.ui.log.Loggers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;

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
	public String createLogFileService(final String loggerName)
	throws RemoteException {
		final Logger logger = LogManager.getLogger(loggerName);
		final String fileName = (
			(RollingRandomAccessFileAppender)
				((org.apache.logging.log4j.core.Logger) logger)
					.getAppenders()
				.get("RollingRandomAccessFile")
		).getFileName();
		return createFileService(fileName);
	}
}
