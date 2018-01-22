package com.emc.mongoose.node;

import com.emc.mongoose.api.model.svc.ServiceBase;
import com.emc.mongoose.scenario.sna.FileManagerService;
import com.emc.mongoose.scenario.sna.FileService;

import java.io.IOException;

public final class BasicFileManagerService
extends ServiceBase
implements FileManagerService {

	public BasicFileManagerService(final int port) {
		super(port);
	}

	@Override
	public String getName() {
		return SVC_NAME;
	}

	@Override
	protected final void doClose() {
	}

	@Override
	public String getFileService(final String path)
	throws IOException {
		final FileService fileSvc = new BasicFileService(path, port);
		fileSvc.start();
		return fileSvc.getName();
	}
}
