package com.emc.mongoose.node;

import com.emc.mongoose.api.model.svc.ServiceBase;
import com.emc.mongoose.scenario.sna.StepManagerService;
import com.emc.mongoose.scenario.sna.StepService;
import com.emc.mongoose.ui.config.Config;

import java.rmi.RemoteException;

public final class BasicStepManagerService
extends ServiceBase
implements StepManagerService {

	public BasicStepManagerService(final int port) {
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
	public final String getStepService(final String stepType, final Config config)
	throws RemoteException {
		final StepService scenarioStepSvc = new BasicStepService(
			port, stepType, config
		);
		scenarioStepSvc.start();
		return scenarioStepSvc.getName();
	}
}
