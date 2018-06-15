package com.emc.mongoose.load.step;

import com.emc.mongoose.svc.Service;

/**
 Remote file access service. All files created are deleted when JVM exits.
 */
public interface StepFileManagerService
extends StepFileManager, Service {

	String SVC_NAME = "file/manager";
}
