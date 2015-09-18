package com.emc.mongoose.core.impl.load.tasks;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 23.12.14.
 */
public final class AwaitLoadJobTask
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final LoadExecutor loadJob;
	private final long timeOut;
	private final TimeUnit timeUnit;
	//
	public AwaitLoadJobTask(final LoadExecutor loadJob, final long timeOut, final TimeUnit timeUnit) {
		this.loadJob = loadJob;
		this.timeOut = timeOut;
		this.timeUnit = timeUnit;
	}
	//
	@Override
	public final void run() {
		try {
			LOG.debug(
				Markers.MSG, "Wait for the remote load service \"{}\" to complete at {}[{}]",
				loadJob.getName(), timeOut, timeUnit
			);
			loadJob.await(timeOut, timeUnit);
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Remote await task failure");
		} catch(final InterruptedException ignore) {
		} finally {
			LOG.debug(Markers.MSG, "Remote await task for \"{}\" was completed", loadJob);
		}
	}
}
