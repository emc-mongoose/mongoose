package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.svc.ServiceUtil;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.config.test.step.node.NodeConfig;
import com.emc.mongoose.ui.log.LogUtil;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class CommandStep
extends StepBase {

	private String cmd = null;

	public CommandStep(
		final Config baseConfig, final List<Map<String, Object>> stepConfigs,
		final Map<String, String> env
	) {
		super(baseConfig, stepConfigs, env);
	}

	private CommandStep(
		final Config baseConfig, final List<Map<String, Object>> stepConfigs,
		final Map<String, String> env, final String cmd
	) {
		super(baseConfig, stepConfigs, env);
		this.cmd = cmd;
	}

	@Override
	protected final void doStart(final Config actualConfig)
	throws InterruptedException {

		final StepConfig stepConfig = actualConfig.getTestConfig().getStepConfig();
		final boolean distributedFlag = stepConfig.getDistributed();

		if(distributedFlag) {

			final NodeConfig nodeConfig = stepConfig.getNodeConfig();
			final int nodePort = nodeConfig.getPort();

			final List<String> nodeAddrs = nodeConfig.getAddrs().stream()
				.map(
					nodeAddr -> nodeAddr.contains(":") ?
						nodeAddr : nodeAddr + ':' + Integer.toString(nodePort)
				)
				.collect(Collectors.toList());

			final List<ScenarioStepManagerService> stepMgrSvcs = nodeAddrs.stream()
				.map(
					nodeAddrWithPort -> {

						ScenarioStepManagerService stepMgrSvc;
						try {
							stepMgrSvc = ServiceUtil.resolve(
								nodeAddrWithPort, ScenarioStepManagerService.SVC_NAME
							);
						} catch(final NotBoundException | IOException | URISyntaxException e) {
							LogUtil.exception(
								Level.WARN, e, "Failed to resolve the service \"{}\" @ {}",
								ScenarioStepManagerService.SVC_NAME, nodeAddrWithPort
							);
							return null;
						}

						String stepSvcName;
						try {
							stepSvcName = stepMgrSvc.getScenarioStepService();
						} catch(final Exception e) {
							LogUtil.exception(
								Level.WARN, e, "Failed to start the new scenario step service @ {}",
								nodeAddrWithPort
							);
							return null;
						}

						ScenarioStepService stepSvc;
						try {
							stepSvc = ServiceUtil.resolve(nodeAddrWithPort, stepSvcName);
						} catch(final IOException | URISyntaxException | NotBoundException e) {
							LogUtil.exception(
								Level.WARN, e, "Failed to resolve the service \"{}\" @ {}",
								ScenarioStepManagerService.SVC_NAME, nodeAddrWithPort
							);
							return null;
						}

						return stepMgrSvc;
					}
				)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		} else {

		}
	}

	@Override
	protected final void doStop() {
	}

	@Override
	protected final void doClose() {
	}

	@Override
	protected final String getTypeName() {
		return "command";
	}

	@Override
	protected final CommandStep copyInstance(final Object config) {
		if(!(config instanceof String)) {
			throw new IllegalArgumentException(
				getTypeName() + " step type accepts only string parameter for the config method"
			);
		}
		return new CommandStep(baseConfig, stepConfigs, env, (String) config);
	}
}
