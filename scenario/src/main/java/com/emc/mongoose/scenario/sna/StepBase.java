package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.concurrent.AsyncRunnableBase;
import com.emc.mongoose.api.model.svc.ServiceUtil;
import com.emc.mongoose.scenario.ScenarioParseException;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.config.test.step.node.NodeConfig;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

public abstract class StepBase
extends AsyncRunnableBase
implements Step, Runnable {

	protected final Config baseConfig;
	protected final List<Map<String, Object>> stepConfigs;
	protected final Map<String, String> env;
	protected final List<StepService> stepSvcs = new ArrayList<>();
	protected boolean distributedFlag = false;
	protected String id;

	protected StepBase(
		final Config baseConfig, final List<Map<String, Object>> stepConfigs,
		final Map<String, String> env
	) {
		this.baseConfig = baseConfig;
		this.stepConfigs = stepConfigs;
		this.env = env;
	}

	@Override
	public final void run() {
		try {
			start();
			try {
				await();
			} catch(final IllegalStateException e) {
				LogUtil.exception(Level.WARN, e, "Failed to await \"{}\"", toString());
			} catch(final InterruptedException e) {
				throw new CancellationException();
			}
		} catch(final IllegalStateException e) {
			LogUtil.exception(Level.WARN, e, "Failed to start \"{}\"", toString());
		} finally {
			try {
				close();
			} catch(final Exception e) {
				LogUtil.exception(Level.WARN, e, "Failed to close \"{}\"", toString());
			}
		}
	}

	@Override
	protected void doStart()
	throws IllegalStateException {
		final Config actualConfig = initConfig();
		final StepConfig stepConfig = actualConfig.getTestConfig().getStepConfig();
		final String stepId = stepConfig.getId();
		try(
			final Instance logCtx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, getClass().getSimpleName())
		) {
			distributedFlag = stepConfig.getDistributed();
			if(distributedFlag) {
				doStartRemote(actualConfig, stepConfig.getNodeConfig());
			} else {
				doStartLocal(actualConfig);
			}
		} catch(final Throwable cause) {
			LogUtil.exception(Level.WARN, cause, "{} step failed", id);
		}
	}

	protected Config initConfig() {
		final String autoStepId = getTypeName() + "_" + LogUtil.getDateTimeStamp() + "_"
			+ hashCode();
		final Config config = new Config(baseConfig);
		if(stepConfigs != null && stepConfigs.size() > 0) {
			for(final Map<String, Object> nextStepConfig: stepConfigs) {
				config.apply(nextStepConfig, autoStepId);
			}
		}
		id = config.getTestConfig().getStepConfig().getId();
		return config;
	}

	protected void doStartRemote(final Config actualConfig, final NodeConfig nodeConfig) {

		final int nodePort = nodeConfig.getPort();

		final List<String> nodeAddrs = nodeConfig.getAddrs().stream()
			.map(
				nodeAddr -> nodeAddr.contains(":") ?
							nodeAddr : nodeAddr + ':' + Integer.toString(nodePort)
			)
			.collect(Collectors.toList());

		final Map<String, Config> configSlices = new HashMap<>();
		sliceConfig(actualConfig, nodeAddrs, configSlices);

		nodeAddrs.forEach(
			nodeAddrWithPort -> {

				StepManagerService stepMgrSvc;
				try {
					stepMgrSvc = ServiceUtil.resolve(
						nodeAddrWithPort, StepManagerService.SVC_NAME
					);
				} catch(final Exception e) {
					LogUtil.exception(
						Level.WARN, e, "Failed to resolve the service \"{}\" @ {}",
						StepManagerService.SVC_NAME, nodeAddrWithPort
					);
					return;
				}

				String stepSvcName;
				try {
					stepSvcName = stepMgrSvc.getStepService(
						getTypeName(), configSlices.get(nodeAddrWithPort)
					);
				} catch(final Exception e) {
					LogUtil.exception(
						Level.WARN, e, "Failed to start the new scenario step service @ {}",
						nodeAddrWithPort
					);
					return;
				}

				StepService stepSvc;
				try {
					stepSvc = ServiceUtil.resolve(nodeAddrWithPort, stepSvcName);
				} catch(final Exception e) {
					LogUtil.exception(
						Level.WARN, e, "Failed to resolve the service \"{}\" @ {}",
						StepManagerService.SVC_NAME, nodeAddrWithPort
					);
					return;
				}

				stepSvcs.add(stepSvc);
			}
		);
	}

	protected abstract void doStartLocal(final Config actualConfig);

	@Override
	protected void doStop() {
		if(distributedFlag) {
			stepSvcs
				.parallelStream()
				.forEach(
					stepSvc -> {
						try {
							stepSvc.stop();
						} catch(final Exception e) {
							try {
								LogUtil.exception(
									Level.WARN, e, "Failed to stop the step service \"{}\"",
									stepSvc.getName()
								);
							} catch(final Exception ignored) {
							}
						}
					}
				);
		}
	}

	@Override
	protected void doClose() {
		if(distributedFlag) {
			stepSvcs
				.parallelStream()
				.forEach(
					stepSvc -> {
						try {
							stepSvc.close();
						} catch(final Exception e) {
							try {
								LogUtil.exception(
									Level.WARN, e, "Failed to close the step service \"{}\"",
									stepSvc.getName()
								);
							} catch(final Exception ignored) {
							}
						}
					}
				);
		}
		stepSvcs.clear();
	}

	protected abstract String getTypeName();

	@Override
	public StepBase config(final Map<String, Object> config) {
		final List<Map<String, Object>> stepConfigsCopy = new ArrayList<>();
		if(stepConfigs != null) {
			stepConfigsCopy.addAll(stepConfigs);
		}
		stepConfigsCopy.add(config);
		return copyInstance(stepConfigsCopy);
	}

	@Override
	public final String id() {
		return id;
	}

	protected abstract StepBase copyInstance(final List<Map<String, Object>> stepConfigs);

	protected void sliceConfig(
		final Config config, final List<String> nodeAddrs, final Map<String, Config> configSlices
	) {
		nodeAddrs.forEach(
			nodeAddrWithPort -> {
				final Config configSlice = new Config(config);
				configSlice.getTestConfig().getStepConfig().setDistributed(false);
				configSlices.put(nodeAddrWithPort, configSlice);
			}
		);
	}
}
