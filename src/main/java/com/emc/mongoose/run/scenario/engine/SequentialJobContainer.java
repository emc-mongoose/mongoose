package com.emc.mongoose.run.scenario.engine;
//
import com.emc.mongoose.common.concurrent.NamingThreadFactory;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.log.Markers;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 02.02.16.
 */
public class SequentialJobContainer
extends JobContainerBase {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final List<JobContainer> subJobs = new LinkedList<>();
	//
	public SequentialJobContainer(final AppConfig appConfig) {
		super(appConfig);
	}
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
		try {
			LOG.debug(Markers.MSG, "{}: start {} sub jobs", toString(), subJobs.size());
			final ThreadFactory tf = new NamingThreadFactory(toString(), true);
			Thread t;
			for(final JobContainer subJob : subJobs) {
				t = tf.newThread(subJob);
				LOG.debug(
					Markers.MSG, "{}: start next sub job \"{}\"", toString(), subJob.toString()
				);
				t.start();
				try {
					if(limitTime > 0) {
						TimeUnit.SECONDS.timedJoin(t, limitTime);
					} else {
						TimeUnit.SECONDS.timedJoin(t, Long.MAX_VALUE);
					}
				} catch(final InterruptedException e) {
					LOG.debug(Markers.MSG, "Interrupted");
					break;
				} finally {
					t.interrupt();
				}
				LOG.debug(Markers.MSG, "{}: sub job \"{}\" is done", toString(), subJob.toString());
			}
			LOG.debug(Markers.MSG, "{}: end", toString());
		} finally {
			subJobs.clear();
		}
	}
}
