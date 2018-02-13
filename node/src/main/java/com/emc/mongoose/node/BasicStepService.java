package com.emc.mongoose.node;

import com.emc.mongoose.api.model.svc.ServiceBase;
import com.emc.mongoose.scenario.sna.LoadStep;
import com.emc.mongoose.scenario.sna.StepService;
import com.emc.mongoose.scenario.sna.Step;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Map;

public final class BasicStepService
extends ServiceBase
implements StepService {

	private final Step step;

	public BasicStepService(final int port, final String stepType, final Config config) {
		super(port);
		switch(stepType) {
			case LoadStep.TYPE:
				step = new LoadStep(config);
				break;
			default:
				throw new IllegalArgumentException("Unexpected step type: " + stepType);
		}
		super.doStart();
	}

	@Override
	protected final void doStart() {
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
	protected void doStop() {
		try {
			step.stop();
		} catch(final IllegalStateException | RemoteException e) {
			try {
				LogUtil.exception(
					Level.ERROR, e, "Failed to stop the wrapped step w/ id: {}", step.id()
				);
			} catch(final RemoteException ignored) {
			}
		}
	}

	@Override
	protected final void doClose()
	throws IOException {
		super.doStop();
		step.close();
	}

	@Override
	public String getName() {
		return SVC_NAME_PREFIX + hashCode();
	}

	@Override
	public Step config(final Map<String, Object> config)
	throws RemoteException {
		return step.config(config);
	}

	@Override
	public final String id()
	throws RemoteException {
		return step.id();
	}
}
