package com.emc.mongoose.load.step.service;

import com.emc.mongoose.load.step.FileManager;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.svc.Service;
import com.emc.mongoose.svc.ServiceUtil;
import org.apache.logging.log4j.Level;

/**
 Remote file access service. All files created are deleted when JVM exits.
 */
public interface FileManagerService
extends FileManager, Service {

	String SVC_NAME = "file/manager";

	static FileManagerService resolve(final String nodeAddrWithPort) {
		try {
			return (FileManagerService) ServiceUtil.resolve(nodeAddrWithPort, FileManagerService.SVC_NAME);
		} catch(final Exception e) {
			LogUtil.exception(Level.ERROR, e, "Failed to resolve the file manager service @ {}", nodeAddrWithPort);
		}
		return null;
	}
}
