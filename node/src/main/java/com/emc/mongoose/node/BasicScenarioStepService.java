package com.emc.mongoose.node;

import com.emc.mongoose.api.model.svc.ServiceBase;
import com.emc.mongoose.scenario.sna.ScenarioStepService;

import java.rmi.RemoteException;

public final class BasicScenarioStepService
extends ServiceBase
implements ScenarioStepService {

	public BasicScenarioStepService(final int port) {
		super(port);
	}

	@Override
	protected final void doClose() {
	}

	@Override
	public String getName()
	throws RemoteException {
		return null;
	}
}
