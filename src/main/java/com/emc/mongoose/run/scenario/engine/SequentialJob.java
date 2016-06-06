package com.emc.mongoose.run.scenario.engine;
//
import com.emc.mongoose.common.concurrent.NamingThreadFactory;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.log.Markers;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 02.02.16.
 */
public class SequentialJob
extends ParentJobBase {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public SequentialJob(final AppConfig appConfig, final Map<String, Object> subTree) {
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
		final ThreadFactory tf = new NamingThreadFactory(toString(), true);
		Thread t;
		for(final Job subJob : childJobs) {
			t = tf.newThread(subJob);
			final long limitTime = localConfig.getLoadLimitTime();
			LOG.debug(
				Markers.MSG, "{}: start next child job \"{}\"", toString(), subJob.toString()
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
			LOG.debug(Markers.MSG, "{}: chile job \"{}\" is done", toString(), subJob.toString());
		}
		LOG.debug(Markers.MSG, "{}: end", toString());
	}
}
