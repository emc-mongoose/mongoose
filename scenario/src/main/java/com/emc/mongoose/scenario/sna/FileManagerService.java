package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.svc.Service;
import com.emc.mongoose.api.model.svc.ServiceUtil;
import com.emc.mongoose.ui.log.LogUtil;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Optional;

public interface FileManagerService
extends Service {

	String SVC_NAME = "file/manager";

	String createFileService(final String path)
	throws IOException;

	static Optional<FileManagerService> resolve(final String nodeAddrWithPort)
	throws RemoteException {
		FileManagerService fileMgrSvc = null;
		try {
			fileMgrSvc = ServiceUtil.resolve(nodeAddrWithPort, FileManagerService.SVC_NAME);
		} catch(final IOException | URISyntaxException | NotBoundException e) {
			LogUtil.exception(
				Level.ERROR, e, "Failed to communicate the file manage service @ {}",
				nodeAddrWithPort
			);
		}
		return Optional.ofNullable(fileMgrSvc);
	}
}
