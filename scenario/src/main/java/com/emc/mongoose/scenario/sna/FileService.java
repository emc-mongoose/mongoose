package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.svc.Service;
import com.emc.mongoose.api.model.svc.ServiceUtil;
import com.emc.mongoose.ui.log.LogUtil;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;

public interface FileService
extends Service {

	String SVC_NAME_PREFIX = "file/";
	byte[] EMPTY = new byte[0];

	byte[] read()
	throws IOException;

	void write(final byte[] buff)
	throws IOException;

	String getFilePath()
	throws RemoteException;

	static Map<String, FileService> resolveFileSvcs(
		final Map<String, StringBuilder> nodeItemsData, final Map<String, FileService> fileSvcs,
		final String nodeAddrWithPort
	) {
		nodeItemsData.put(nodeAddrWithPort, new StringBuilder());
		try {
			final FileManagerService fileMgrSvc = ServiceUtil.resolve(
				nodeAddrWithPort, FileManagerService.SVC_NAME
			);
			try {
				final String fileSvcName = fileMgrSvc.getFileService(null);
				try {
					final FileService fileSvc = ServiceUtil.resolve(
						nodeAddrWithPort, fileSvcName
					);
					fileSvcs.put(nodeAddrWithPort, fileSvc);
				} catch(
					final NotBoundException | RemoteException | MalformedURLException |
						URISyntaxException e
					) {
					LogUtil.exception(
						Level.ERROR, e, "Failed to communicate the file service \"{}\" @ {}",
						fileSvcName, nodeAddrWithPort
					);
				}
			} catch(final IOException e) {
				LogUtil.exception(
					Level.ERROR, e, "Failed to create the remote file service @ {}",
					nodeAddrWithPort
				);
			}
		} catch(
			final NotBoundException | RemoteException | MalformedURLException |
				URISyntaxException e
			) {
			LogUtil.exception(
				Level.ERROR, e, "Failed to communicate the file manage service @ {}",
				nodeAddrWithPort
			);
		}
		return fileSvcs;
	}

	static String writeItemData(
		final Map<String, StringBuilder> nodeItemsData, final Map<String, FileService> fileSvcs,
		final String nodeAddrWithPort
	) {
		final FileService fileSvc = fileSvcs.get(nodeAddrWithPort);
		final String itemsData = nodeItemsData.get(nodeAddrWithPort).toString();
		try {
			fileSvc.write(itemsData.getBytes());
		} catch(final IOException e) {
			try {
				LogUtil.exception(
					Level.WARN, e, "Failed to write the items data to the remote file {}",
					fileSvc.getName()
				);
			} catch(final RemoteException ignored) {
			}
		}
		return nodeAddrWithPort;
	}
}
