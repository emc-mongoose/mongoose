package com.emc.mongoose.node;

import com.emc.mongoose.api.model.svc.ServiceBase;
import com.emc.mongoose.scenario.ScenarioParseException;
import com.emc.mongoose.scenario.sna.CommandStep;
import com.emc.mongoose.scenario.sna.StepService;
import com.emc.mongoose.scenario.sna.Step;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.rmi.RemoteException;

public final class BasicStepService
extends ServiceBase
implements StepService {

	private volatile Step step;

	public BasicStepService(final int port, final String stepType, final Config config) {
		super(port);
		switch(stepType) {
			case CommandStep.TYPE:
				step = new CommandStep(config);
				break;
			default:
				throw new IllegalArgumentException("Unexpected step type: " + stepType);
		}
	}

	@Override
	protected final void doStart() {
		super.doStart();
		try {
			step.start();
		} catch(final IllegalStateException | RemoteException e) {
			try {
				LogUtil.exception(
					Level.ERROR, e, "Failed to start the wrapped step w/ id: {}", step.id()
				);
			} catch(final RemoteException ignored) {
			}
		}
	}

	@Override
	protected final void doClose()
	throws IOException {
		step.close();
	}

	@Override
	public String getName() {
		return SVC_NAME_PREFIX + hashCode();
	}

	@Override
	public Step config(final Object config)
	throws ScenarioParseException, RemoteException {
		return step.config(config);
	}

	@Override
	public final String id()
	throws RemoteException {
		return step.id();
	}
}
