package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.svc.Service;

import java.io.IOException;

public interface FileManagerService
extends Service {

	String SVC_NAME = "file/manager";

	String getFileService(final String path)
	throws IOException;
}
