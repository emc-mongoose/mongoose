package com.emc.mongoose.run.scenario.engine;
//
import com.emc.mongoose.common.conf.TimeUtil;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.common.log.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.concurrent.TimeUnit;
/**
 Created by andrey on 07.04.16.
 */
public class SleepJobContainer
implements JobContainer {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final long timeValue;
	private final TimeUnit timeUnit;
	//
	public SleepJobContainer(final String sleepTimeSpec)
	throws IllegalArgumentException {
		if(sleepTimeSpec == null || sleepTimeSpec.isEmpty()) {
			throw new IllegalArgumentException(sleepTimeSpec);
		}
		timeValue = TimeUtil.getTimeValue(sleepTimeSpec);
		timeUnit = TimeUtil.getTimeUnit(sleepTimeSpec);
	}
	//
	@Override
	public boolean append(final JobContainer subJob) {
		throw new IllegalStateException("Appending sub jobs to a sleep step is not allowed");
	}
	//
	@Override
	public void run() {
		try {
			LOG.info(Markers.MSG, "Invoking sleep step: {} {}", timeValue, timeUnit.name());
			timeUnit.sleep(timeValue);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.INFO, e, "Sleep step was interrupted");
		}
	}
}
