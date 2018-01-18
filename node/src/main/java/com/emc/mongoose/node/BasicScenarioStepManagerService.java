package com.emc.mongoose.node;

import com.emc.mongoose.api.model.svc.ServiceBase;
import com.emc.mongoose.scenario.sna.ScenarioStepManagerService;
import com.emc.mongoose.scenario.sna.ScenarioStepService;

import java.rmi.RemoteException;

public final class BasicScenarioStepManagerService
extends ServiceBase
implements ScenarioStepManagerService {

	public BasicScenarioStepManagerService(final int port) {
		super(port);
	}

	@Override
	public String getName()
	throws RemoteException {
		return SVC_NAME;
	}

	@Override
	protected final void doClose() {
	}

	@Override
	public final String getScenarioStepService()
	throws Exception {
		final ScenarioStepService scenarioStepSvc = new BasicScenarioStepService(port);
		scenarioStepSvc.start();
		return scenarioStepSvc.getName();
	}
}
