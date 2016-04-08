package com.emc.mongoose.run.scenario.engine;
//
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 02.02.16.
 */
public class ParallelJobContainer
extends JobContainerBase {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final List<JobContainer> subJobs = new LinkedList<>();
	//
	protected ParallelJobContainer(final AppConfig appConfig) {
		super(appConfig);
	}
	//
	@Override
	public final synchronized void run() {
		final ExecutorService parallelJobsExecutor = Executors.newFixedThreadPool(
			subJobs.size(), new GroupThreadFactory("jobContainerWorker" + hashCode(), true)
		);
		for(final JobContainer subJob : subJobs) {
			parallelJobsExecutor.submit(subJob);
		}
		LOG.debug(Markers.MSG, "{}: started {} sub jobs", toString(), subJobs.size());
		parallelJobsExecutor.shutdown();
		try {
			parallelJobsExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
			LOG.debug(Markers.MSG, "{}: {} sub jobs done", toString(), subJobs.size());
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.WARN, e, "{}: interrupted the sub jobs execution");
		}
	}
	//
	@Override
	public String toString() {
		return "parallelJobContainer#" + hashCode();
	}
	//
	@Override
	public final synchronized boolean append(final JobContainer subJob) {
		return subJobs.add(subJob);
	}
}
