package com.emc.mongoose.scenario.step.node;

import com.emc.mongoose.api.common.env.Extensions;
import com.emc.mongoose.api.metrics.MetricsSnapshot;
import com.emc.mongoose.api.model.svc.ServiceBase;
import com.emc.mongoose.scenario.step.LoadStep;
import com.emc.mongoose.scenario.step.LoadStepService;
import com.emc.mongoose.scenario.step.type.LoadStepFactory;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

public final class BasicLoadStepService
extends ServiceBase
implements LoadStepService {

	private final LoadStep loadStep;

	public BasicLoadStepService(
		final int port, final String stepType, final Config config,
		final List<Map<String, Object>> stepConfigs
	) {
		super(port);
		// don't override the test-step-id value on the remote node again
		config.getTestConfig().getStepConfig().setIdTmp(false);

		final ServiceLoader<LoadStepFactory> loader = ServiceLoader.load(
			LoadStepFactory.class, Extensions.CLS_LOADER
		);
		LoadStepFactory selectedFactory = null;
		final List<String> typeNames = new ArrayList<>();
		for(final LoadStepFactory factory : loader) {
			final String nextImplTypeName = factory.getTypeName();
			typeNames.add(nextImplTypeName);
			if(stepType.endsWith(nextImplTypeName)) {
				selectedFactory = factory;
				break;
			}
		}
		if(selectedFactory == null) {
			throw new IllegalStateException(
				"Failed to find the load step implementation for type \"" + stepType +
					"\", available types: " + Arrays.toString(typeNames.toArray())
			);
		}

		try(
			final CloseableThreadContext.Instance logCtx = CloseableThreadContext
				.put(KEY_CLASS_NAME, BasicLoadStepService.class.getSimpleName())
		) {
			loadStep = selectedFactory.create(config, stepConfigs);
			Loggers.MSG.info(
			"New step service for \"{}\"", config.getTestConfig().getStepConfig().getId()
			);
			super.doStart();
		}
	}

	@Override
	protected final void doStart() {
		try(
			final CloseableThreadContext.Instance logCtx = CloseableThreadContext
				.put(KEY_CLASS_NAME, BasicLoadStepService.class.getSimpleName())
				.put(KEY_TEST_STEP_ID, loadStep.id())
		) {
			loadStep.start();
			Loggers.MSG.info("Step service for \"{}\" is started", loadStep.id());
		} catch(final IllegalStateException | RemoteException e) {
			try {
				LogUtil.exception(
					Level.ERROR, e, "Failed to start the wrapped step w/ id: {}", loadStep.id()
				);
			} catch(final RemoteException ignored) {
			}
		}
	}

	@Override
	protected void doStop() {
		try(
			final CloseableThreadContext.Instance logCtx = CloseableThreadContext
				.put(KEY_CLASS_NAME, BasicLoadStepService.class.getSimpleName())
				.put(KEY_TEST_STEP_ID, loadStep.id())
		) {
			loadStep.stop();
			Loggers.MSG.info("Step service for \"{}\" is stopped", loadStep.id());
		} catch(final IllegalStateException | RemoteException e) {
			try {
				LogUtil.exception(
					Level.ERROR, e, "Failed to stop the wrapped step w/ id: {}", loadStep.id()
				);
			} catch(final RemoteException ignored) {
			}
		}
	}

	@Override
	protected final void doClose()
	throws IOException {
		try(
			final CloseableThreadContext.Instance logCtx = CloseableThreadContext
				.put(KEY_CLASS_NAME, BasicLoadStepService.class.getSimpleName())
				.put(KEY_TEST_STEP_ID, loadStep.id())
		) {
			super.doStop();
			loadStep.close();
			Loggers.MSG.info("Step service for \"{}\" is closed", loadStep.id());
		}
	}

	@Override
	public String name() {
		return SVC_NAME_PREFIX + hashCode();
	}

	@Override
	public LoadStep config(final Map<String, Object> config)
	throws RemoteException {
		return loadStep.config(config);
	}

	@Override
	public final String id()
	throws RemoteException {
		return loadStep.id();
	}

	@Override
	public final String getTypeName()
	throws RemoteException {
		return loadStep.getTypeName();
	}

	@Override
	public final List<MetricsSnapshot> metricsSnapshots()
	throws RemoteException {
		return loadStep.metricsSnapshots();
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws IllegalStateException, InterruptedException {
		try(
			final CloseableThreadContext.Instance logCtx = CloseableThreadContext
				.put(KEY_CLASS_NAME, BasicLoadStepService.class.getSimpleName())
				.put(KEY_TEST_STEP_ID, loadStep.id())
		) {
			return loadStep.await(timeout, timeUnit);
		} catch(final RemoteException ignored) {
		}
		return false;
	}
}
