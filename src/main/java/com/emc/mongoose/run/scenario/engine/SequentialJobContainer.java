package com.emc.mongoose.run.scenario.engine;
//
import java.util.LinkedList;
import java.util.List;
/**
 Created by kurila on 02.02.16.
 */
public class SequentialJobContainer
implements JobContainer {
	//
	private final List<JobContainer> subJobs = new LinkedList<>();
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
	public final synchronized void run() {
		for(final JobContainer subJob : subJobs) {
			subJob.run();
		}
	}
}
