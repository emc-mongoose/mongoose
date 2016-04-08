package com.emc.mongoose.run.scenario.engine;
//
import com.emc.mongoose.common.log.Markers;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.LinkedList;
import java.util.List;
/**
 Created by kurila on 02.02.16.
 */
public class SequentialJobContainer
implements JobContainer {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final List<JobContainer> subJobs = new LinkedList<>();
	//
	@Override
	public final synchronized boolean append(final JobContainer subJob) {
		return subJobs.add(subJob);
	}
	//
	@Override
	public String toString() {
		return "sequentialJobContainer#" + hashCode();
	}
	//
	@Override
	public synchronized void run() {
		LOG.debug(Markers.MSG, "{}: start {} sub jobs", toString(), subJobs.size());
		for(final JobContainer subJob : subJobs) {
			LOG.debug(Markers.MSG, "{}: start next sub job \"{}\"", toString(), subJob.toString());
			subJob.run();
			LOG.debug(Markers.MSG, "{}: sub job \"{}\" is done", toString(), subJob.toString());
		}
		LOG.debug(Markers.MSG, "{}: end", toString());
	}
}
