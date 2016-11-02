package com.emc.mongoose.ui.scenario;

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

	private final static Logger LOG = LogManager.getLogger();

	protected final List<Job> childJobs = new LinkedList<>();

	protected ParentJobBase(final Config appConfig, final Map<String, Object> subTree) {
		super(appConfig);
		loadSubTree(subTree);
	}

	protected void loadSubTree(final Map<String, Object> subTree) {
		final Object nodeConfig = subTree.get(KEY_NODE_CONFIG);
		if(nodeConfig != null) {
			if(nodeConfig instanceof Map) {
				localConfig.apply((Map<String, Object>) nodeConfig);
			} else {
				LOG.error(Markers.ERR, "Invalid config node type: {}", nodeConfig.getClass());
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
							LOG.error(Markers.ERR, "Invalid job node type: {}", job.getClass());
						}
					} else {
						LOG.warn(Markers.ERR, "{}: job node is null");
					}
				}
			} else {
				LOG.error(Markers.ERR, "Invalid jobs node type: {}", jobList.getClass());
			}
		}
	}

	protected void appendNewJob(final Map<String, Object> subTree, final Config config) {
		LOG.debug(Markers.MSG, "Load the subtree to the job \"{}\"", this.toString());
		final String jobType = (String) subTree.get(KEY_NODE_TYPE);
		if(jobType == null) {
			LOG.fatal(Markers.ERR, "No \"{}\" element for the job", KEY_NODE_TYPE);
		} else {
			switch(jobType) {
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
				default:
					LOG.warn(
						Markers.ERR, "{}: unexpected job type value: {}", this, jobType
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
