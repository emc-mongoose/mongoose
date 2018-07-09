package com.emc.mongoose.load.step.service.file;

import com.emc.mongoose.load.step.file.FileManager;
import com.emc.mongoose.load.step.file.FileManagerImpl;
import com.emc.mongoose.svc.ServiceBase;
import static com.emc.mongoose.Constants.KEY_CLASS_NAME;

import static org.apache.logging.log4j.CloseableThreadContext.put;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;

import java.io.IOException;

public final class FileManagerServiceImpl
extends ServiceBase
implements FileManagerService {

	private final FileManager localFileMgr = new FileManagerImpl();

	public FileManagerServiceImpl(final int port) {
		super(port);
	}

	@Override
	public String name() {
		return SVC_NAME;
	}

	@Override
	protected final void doStart() {
		try(final Instance logCtx = put(KEY_CLASS_NAME, getClass().getSimpleName())) {
			super.doStart();
		}
	}

	@Override
	public final String logFileName(final String loggerName, final String testStepId)
	throws IOException {
		return localFileMgr.logFileName(loggerName, testStepId);
	}

	@Override
	public final String newTmpFileName()
	throws IOException {
		return localFileMgr.newTmpFileName();
	}

	@Override
	public final byte[] readFromFile(final String fileName, final long offset)
	throws IOException {
		return localFileMgr.readFromFile(fileName, offset);
	}

	@Override
	public final void writeToFile(final String fileName, final byte[] buff)
	throws IOException {
		localFileMgr.writeToFile(fileName, buff);
	}

	@Override
	public final long fileSize(final String fileName)
	throws IOException {
		return localFileMgr.fileSize(fileName);
	}

	@Override
	public final void truncateFile(final String fileName, final long size)
	throws IOException {
		localFileMgr.truncateFile(fileName, size);
	}

	@Override
	public final void deleteFile(final String fileName)
	throws IOException {
		localFileMgr.deleteFile(fileName);
	}
}
