package com.emc.mongoose.run.scenario;

import com.emc.mongoose.ui.config.Config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
/**
 Created by andrey on 08.11.16.
 */
public class MixedLoadJob
extends JobBase {

	private static final Logger LOG = LogManager.getLogger();

	public MixedLoadJob(final Config appConfig, final Map<String, Object> subTree) {
		super(appConfig);

		final List<Map<String, Object>>
			nodeConfigList = (List<Map<String, Object>>) subTree.get(KEY_NODE_CONFIG);
		if(nodeConfigList != null && nodeConfigList.size() > 0) {
		} else {
			throw new IllegalArgumentException("Configuration list is empty");
		}

		final List<Integer> weights = (List<Integer>) subTree.get(KEY_NODE_WEIGHTS);
		if(weights != null) {
			if(weights.size() != nodeConfigList.size()) {
				throw new IllegalArgumentException("Weights count is not equal to sub-jobs count");
			}
		}
	}

	@Override
	public void close()
	throws IOException {
	}
}
