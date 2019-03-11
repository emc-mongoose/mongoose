package com.emc.mongoose.base.load.step.service.file;

import com.emc.mongoose.base.load.step.file.FileManager;
import com.emc.mongoose.base.svc.Service;

/** Remote file access service. All files created are deleted when JVM exits. */
public interface FileManagerService extends FileManager, Service {

	String SVC_NAME = "file/manager";
}
