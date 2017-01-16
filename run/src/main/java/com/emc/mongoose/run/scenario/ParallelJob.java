package com.emc.mongoose.run.scenario;

import com.emc.mongoose.ui.log.NamingThreadFactory;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.Markers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 02.02.16.
 */
public class ParallelJob
extends ParentJobBase {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	protected ParallelJob(final Config appConfig, final Map<String, Object> subTree)
	throws ScenarioParseException {
		super(appConfig, subTree);
	}
	//
	@Override
	public final synchronized void run() {
		
		super.run();

		final ExecutorService parallelJobsExecutor = Executors.newFixedThreadPool(
			childJobs.size(), new NamingThreadFactory("jobWorker" + hashCode(), true)
		);
		for(final Job subJob : childJobs) {
			parallelJobsExecutor.submit(subJob);
		}
		LOG.info(
			Markers.MSG, "{}: execute {} child jobs in parallel", toString(), childJobs.size()
		);
		parallelJobsExecutor.shutdown();
		
		final long limitTime = localConfig.getLoadConfig().getLimitConfig().getTime();
		try {
			if(limitTime > 0) {
				parallelJobsExecutor.awaitTermination(limitTime, TimeUnit.SECONDS);
			} else {
				parallelJobsExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
			}
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "{}: interrupted the child jobs execution", toString());
		} finally {
			parallelJobsExecutor.shutdownNow();
		}
		LOG.info(
			Markers.MSG, "{}: finished parallel execution of {} child jobs", toString(),
			childJobs.size()
		);
	}
	//
	@Override
	public String toString() {
		return "parallelJob#" + hashCode();
	}
}
