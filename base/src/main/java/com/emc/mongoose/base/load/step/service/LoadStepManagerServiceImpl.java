package com.emc.mongoose.base.load.step.service;

import static com.emc.mongoose.base.Constants.KEY_CLASS_NAME;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;

import com.emc.mongoose.base.env.Extension;
import com.emc.mongoose.base.load.step.LoadStepManagerService;
import com.emc.mongoose.base.logging.Loggers;
import com.emc.mongoose.base.metrics.MetricsManager;
import com.emc.mongoose.base.svc.ServiceBase;
import com.github.akurilov.confuse.Config;
import java.rmi.RemoteException;
import java.util.List;
import org.apache.logging.log4j.CloseableThreadContext;

public final class LoadStepManagerServiceImpl extends ServiceBase
				implements LoadStepManagerService {

	private final List<Extension> extensions;
	private final MetricsManager metricsMgr;

	public LoadStepManagerServiceImpl(
					final int port, final List<Extension> extensions, final MetricsManager metricsMgr) {
		super(port);
		this.extensions = extensions;
		this.metricsMgr = metricsMgr;
	}

	@Override
	public final String name() {
		return SVC_NAME;
	}

	@Override
	protected final void doStart() {
		try (final Instance logCtx = CloseableThreadContext.put(KEY_CLASS_NAME, getClass().getSimpleName())) {
			super.doStart();
		}
	}

	@Override
	protected final void doClose() {
		Loggers.MSG.info("Service \"{}\" closed", SVC_NAME);
	}

	@Override
	public final String getStepService(
					final String stepType, final Config config, final List<Config> ctxConfigs)
					throws RemoteException {
		final LoadStepService stepSvc = new LoadStepServiceImpl(port, extensions, stepType, config, ctxConfigs, metricsMgr);
		Loggers.MSG.info("New step service started @ port #{}: {}", port, stepSvc.name());
		return stepSvc.name();
	}
}
