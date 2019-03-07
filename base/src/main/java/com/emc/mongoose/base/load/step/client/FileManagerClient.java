package com.emc.mongoose.base.load.step.client;

import com.emc.mongoose.base.load.step.service.file.FileManagerService;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.svc.ServiceUtil;
import org.apache.logging.log4j.Level;

public interface FileManagerClient {
	static FileManagerService resolve(final String nodeAddrWithPort) {
		try {
			return (FileManagerService) ServiceUtil.resolve(nodeAddrWithPort, FileManagerService.SVC_NAME);
		} catch (final Exception e) {
			LogUtil.exception(
							Level.ERROR, e, "Failed to resolve the file manager service @ {}", nodeAddrWithPort);
		}
		return null;
	}
}
