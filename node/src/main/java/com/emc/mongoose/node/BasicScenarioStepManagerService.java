package com.emc.mongoose.node;

import com.emc.mongoose.api.model.svc.ServiceUtil;
import com.emc.mongoose.scenario.sna.ScenarioStepManagerService;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

public final class BasicScenarioStepManagerService
implements ScenarioStepManagerService {

	private final int port;

	public BasicScenarioStepManagerService(final int port) {
		this.port = port;
		ServiceUtil.create(this, port);
	}

	@Override
	public int getRegistryPort()
	throws RemoteException {
		return port;
	}

	@Override
	public String getName()
	throws RemoteException {
		return SVC_NAME;
	}

	@Override
	public final void close()
	throws RemoteException, MalformedURLException {
		ServiceUtil.close(this);
	}


}
