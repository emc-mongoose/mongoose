package com.emc.mongoose.scenario.step.server;

import com.emc.mongoose.api.model.svc.ServiceBase;
import com.emc.mongoose.scenario.step.svc.LoadStepManagerService;
import com.emc.mongoose.scenario.step.svc.LoadStepService;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.Loggers;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public final class BasicLoadStepManagerService
extends ServiceBase
implements LoadStepManagerService {

	public BasicLoadStepManagerService(final int port) {
		super(port);
	}

	@Override
	public final String name() {
		return SVC_NAME;
	}

	@Override
	protected final void doStart() {
		super.doStart();
		Loggers.MSG.info("Service \"{}\" started @ port #{}", SVC_NAME, port);
	}

	@Override
	protected final void doClose() {
		Loggers.MSG.info("Service \"{}\" closed", SVC_NAME);
	}

	@Override
	public final String getStepService(
		final String stepType, final Config config, final List<Map<String, Object>> stepConfigs
	) throws RemoteException {
		final LoadStepService stepSvc = new BasicLoadStepService(
			port, stepType, config, stepConfigs
		);
		Loggers.MSG.info("New step service started @ port #{}: {}", port, stepSvc.name());
		return stepSvc.name();
	}
}
