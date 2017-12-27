package com.emc.mongoose.scenario.step.local;

import com.emc.mongoose.scenario.step.StepSlice;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

public abstract class StepBase
implements Step {

	protected final Config baseConfig;
	protected final Map<String, String> env;
	protected String id;

	protected StepBase(final Config baseConfig, final Map<String, String> env) {
		this.baseConfig = baseConfig;
		this.env = env;
	}

	protected Config init() {
		final String autoStepId = getTypeName() + "_" + LogUtil.getDateTimeStamp() + "_"
			+ hashCode();
		final Config config = new Config(baseConfig);
		config.apply(Collections.emptyMap(), autoStepId);
		id = config.getTestConfig().getStepConfig().getId();
		return config;
	}

	@Override
	public void start()
	throws IllegalStateException, RemoteException {
		final Config actualConfig = init();
		final String stepId = actualConfig.getTestConfig().getStepConfig().getId();
		try(
			final CloseableThreadContext.Instance logCtx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, getClass().getSimpleName())
		) {
			start(actualConfig);
		} catch(final InterruptedException e) {
			throw new CancellationException();
		} catch(final Throwable cause) {
			LogUtil.exception(Level.WARN, cause, "{} step failed", id);
		} finally {
			try {
				close();
			} catch(final IOException e) {
				LogUtil.exception(Level.WARN, e, "{} step failed to close", id);
			}
		}
	}

	@Override
	public void stop()
	throws IllegalStateException, RemoteException {
	}

	@Override
	public void close()
	throws IOException {
	}

	protected List<StepSlice> slice() {
		return null;
	}

	protected abstract String getTypeName();
}
