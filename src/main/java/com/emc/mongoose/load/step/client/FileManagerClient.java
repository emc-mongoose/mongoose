package com.emc.mongoose.load.step.client;

import com.emc.mongoose.load.step.service.file.FileManagerService;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.svc.ServiceUtil;

import org.apache.logging.log4j.Level;

public interface FileManagerClient {
	static FileManagerService resolve(final String nodeAddrWithPort) {
		try {
			return (FileManagerService) ServiceUtil.resolve(nodeAddrWithPort, FileManagerService.SVC_NAME);
		} catch(final Exception e) {
			LogUtil.exception(Level.ERROR, e, "Failed to resolve the file manager service @ {}", nodeAddrWithPort);
		}
		return null;
	}
}
