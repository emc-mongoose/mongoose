package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.config.test.step.node.NodeConfig;

import java.util.List;

public interface StepServiceUtil {

	String SVC_NAME_PREFIX = "scenario/step/";

	static void resolve(
		final List<StepService> stepServices, final StepConfig stepConfig,
		final List<StepSlice> stepSlices
	) {
		final boolean distributedFlag = stepConfig.getDistributed();
		if(distributedFlag) {
			final NodeConfig nodeConfig = stepConfig.getNodeConfig();
			final int nodePort = nodeConfig.getPort();
			final List<String> nodeAddrs = nodeConfig
				.getAddrs()
				.stream()
				.map(
					nodeAddr -> nodeAddr.contains(":") ?
						nodeAddr : nodeAddr + ':' + Integer.toString(nodePort)
				)
				.forEach(

				);
		} else {

		}
	}
}
