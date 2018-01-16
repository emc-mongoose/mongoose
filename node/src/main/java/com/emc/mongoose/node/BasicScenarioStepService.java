package com.emc.mongoose.node;

import com.emc.mongoose.api.model.svc.ServiceUtil;
import com.emc.mongoose.scenario.sna.ScenarioStepService;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

public final class BasicScenarioStepService
implements ScenarioStepService {

	private final int port;

	public BasicScenarioStepService(final int port) {
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
