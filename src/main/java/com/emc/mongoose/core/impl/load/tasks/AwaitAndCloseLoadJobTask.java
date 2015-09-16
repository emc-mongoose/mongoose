package com.emc.mongoose.core.impl.load.tasks;

import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by gusakk on 16.09.15.
 */
public class AwaitAndCloseLoadJobTask
extends AwaitLoadJobTask {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public AwaitAndCloseLoadJobTask(
		final LoadExecutor loadJob, final long timeOut, final TimeUnit timeUnit
	) {
		super(loadJob, timeOut, timeUnit);
	}
	//
	@Override
	public void run() {
		super.run();
		try {
			loadJob.close();
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to close the loadJob \"{}\"", loadJob
			);
		}
	}
}
