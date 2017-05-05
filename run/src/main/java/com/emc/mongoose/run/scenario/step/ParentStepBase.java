package com.emc.mongoose.run.scenario.step;

import com.emc.mongoose.run.scenario.ScenarioParseException;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.Loggers;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 Created by andrey on 06.06.16.
 */
public abstract class ParentStepBase
extends StepBase {

	protected final List<Step> subSteps = new LinkedList<>();

	protected ParentStepBase(final Config appConfig, final Map<String, Object> subTree)
	throws ScenarioParseException {
		super(appConfig);
		loadSubTree(subTree);
	}

	protected void loadSubTree(final Map<String, Object> subTree)
	throws ScenarioParseException {
		final Object nodeConfig = subTree.get(KEY_NODE_CONFIG);
		if(nodeConfig != null) {
			if(nodeConfig instanceof Map) {
				localConfig.apply((Map<String, Object>) nodeConfig);
			} else {
				throw new ScenarioParseException(
					"Invalid config node type: \"" + nodeConfig.getClass() + "\""
				);
			}
		}
		final Object jobList = subTree.get(KEY_NODE_JOBS);
		if(jobList != null) {
			if(jobList instanceof List) {
				for(final Object job : (List) jobList) {
					if(job != null) {
						if(job instanceof Map) {
							appendNewJob((Map<String, Object>) job, localConfig);
						} else {
							throw new ScenarioParseException(
								"Invalid job node type: \"" + job.getClass() + "\""
							);
						}
					} else {
						throw new ScenarioParseException("job node is null");
					}
				}
			} else {
				throw new ScenarioParseException(
					"Invalid jobs node type: \"" + jobList.getClass() + "\""
				);
			}
		}
	}

	protected void appendNewJob(final Map<String, Object> subTree, final Config config)
	throws ScenarioParseException {
		Loggers.MSG.debug("Load the subtree to the step \"{}\"", this.toString());
		final String jobType = (String) subTree.get(KEY_NODE_TYPE);
		if(jobType == null) {
			throw new ScenarioParseException("No \"" + KEY_NODE_TYPE + "\" element for the job");
		} else {
			switch(jobType) {
				case NODE_TYPE_CHAIN:
					append(new ChainStep(config, subTree));
					break;
				case NODE_TYPE_COMMAND:
					append(new CommandStep(config, subTree));
					break;
				case NODE_TYPE_FOR:
					append(new ForStep(config, subTree));
					break;
				case NODE_TYPE_LOAD:
					append(new LoadStep(config, subTree, false));
					break;
				case NODE_TYPE_PARALLEL:
					append(new ParallelStep(config, subTree));
					break;
				case NODE_TYPE_PRECONDITION:
					append(new LoadStep(config, subTree, true));
					break;
				case NODE_TYPE_SEQUENTIAL:
					append(new SequentialStep(config, subTree));
					break;
				case NODE_TYPE_MIXED:
					append(new MixedLoadStep(config, subTree));
					break;
				default:
					throw new ScenarioParseException(
						"\"" + this.toString() + "\": unexpected job type value: " + jobType
					);
			}
		}
	}

	protected synchronized boolean append(final Step subStep) {
		return subSteps.add(subStep);
	}

	@Override
	public void close()
	throws IOException {
		try {
			for(final Step subStep : subSteps) {
				subStep.close();
			}
		} finally {
			subSteps.clear();
		}
	}
}
