package com.emc.mongoose.scenario.step.node;

import com.emc.mongoose.api.model.svc.ServiceBase;
import com.emc.mongoose.scenario.step.LoadStepManagerService;
import com.emc.mongoose.scenario.step.LoadStepService;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.Loggers;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;

import org.apache.logging.log4j.CloseableThreadContext;

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
		try(
			final CloseableThreadContext.Instance logCtx = CloseableThreadContext
				.put(KEY_CLASS_NAME, BasicLoadStepManagerService.class.getSimpleName())
		) {
			super.doStart();
			Loggers.MSG.info("Service \"{}\" started @ port #{}", SVC_NAME, port);
		}
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
