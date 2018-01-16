package com.emc.mongoose.node;

import com.emc.mongoose.api.model.svc.ServiceBase;
import com.emc.mongoose.scenario.sna.FileManagerService;

import java.rmi.RemoteException;

public final class BasicFileManagerService
extends ServiceBase
implements FileManagerService {

	public BasicFileManagerService(final int port) {
		super(port);
		start();
	}

	@Override
	public String getName()
	throws RemoteException {
		return SVC_NAME;
	}
}
