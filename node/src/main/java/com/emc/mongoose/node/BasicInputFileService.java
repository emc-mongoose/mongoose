package com.emc.mongoose.node;

import com.emc.mongoose.api.model.svc.ServiceBase;
import com.emc.mongoose.scenario.sna.InputFileService;

import java.rmi.RemoteException;

public final class BasicInputFileService
extends ServiceBase
implements InputFileService {

	public BasicInputFileService(final int port) {
		super(port);
		start();
	}

	@Override
	public String getName()
	throws RemoteException {
		return SVC_NAME;
	}
}
