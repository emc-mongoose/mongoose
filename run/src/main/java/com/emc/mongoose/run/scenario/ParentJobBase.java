package com.emc.mongoose.run.scenario;

import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 Created by andrey on 06.06.16.
 */
public abstract class ParentJobBase
extends JobBase {

	private static final Logger LOG = LogManager.getLogger();

	protected final List<Job> childJobs = new LinkedList<>();

	protected ParentJobBase(final Config appConfig, final Map<String, Object> subTree)
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
		LOG.debug(Markers.MSG, "Load the subtree to the job \"{}\"", this.toString());
		final String jobType = (String) subTree.get(KEY_NODE_TYPE);
		if(jobType == null) {
			throw new ScenarioParseException("No \"" + KEY_NODE_TYPE + "\" element for the job");
		} else {
			switch(jobType) {
				case NODE_TYPE_CHAIN:
					append(new ChainJob(config, subTree));
					break;
				case NODE_TYPE_COMMAND:
					append(new CommandJob(config, subTree));
					break;
				case NODE_TYPE_FOR:
					append(new ForJob(config, subTree));
					break;
				case NODE_TYPE_LOAD:
					append(new LoadJob(config, subTree, false));
					break;
				case NODE_TYPE_PARALLEL:
					append(new ParallelJob(config, subTree));
					break;
				case NODE_TYPE_PRECONDITION:
					append(new LoadJob(config, subTree, true));
					break;
				case NODE_TYPE_SEQUENTIAL:
					append(new SequentialJob(config, subTree));
					break;
				case NODE_TYPE_MIXED:
					append(new MixedLoadJob(config, subTree));
					break;
				default:
					throw new ScenarioParseException(
						"\"" + this.toString() + "\": unexpected job type value: " + jobType
					);
			}
		}
	}

	protected synchronized boolean append(final Job subJob) {
		return childJobs.add(subJob);
	}

	@Override
	public void close()
	throws IOException {
		try {
			for(final Job subJob : childJobs) {
				subJob.close();
			}
		} finally {
			childJobs.clear();
		}
	}
}
