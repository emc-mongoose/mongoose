package com.emc.mongoose.run.scenario;

import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 Created by kurila on 02.02.16.
 */
public class SequentialJob
extends ParentJobBase {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public SequentialJob(final Config appConfig, final Map<String, Object> subTree) {
		super(appConfig, subTree);
	}
	//
	@Override
	public String toString() {
		return "sequentialJob#" + hashCode();
	}
	//
	@Override
	public synchronized void run() {
		LOG.debug(Markers.MSG, "{}: start {} child jobs", toString(), childJobs.size());
		for(final Job subJob : childJobs) {
			LOG.debug(Markers.MSG, "{}: child job \"{}\" start", toString(), subJob.toString());
			subJob.run();
			LOG.debug(Markers.MSG, "{}: child job \"{}\" is done", toString(), subJob.toString());
		}
		LOG.debug(Markers.MSG, "{}: end", toString());
	}
}
