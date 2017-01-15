package com.emc.mongoose.run.scenario;

import com.emc.mongoose.ui.config.Config;
import static com.emc.mongoose.ui.config.Config.LoadConfig.JobConfig;
import static com.emc.mongoose.common.Constants.KEY_JOB_NAME;

import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

/**
 Created by kurila on 08.04.16.
 */
public abstract class JobBase
implements Job {

	private static final Logger LOG = LogManager.getLogger();

	protected final Config localConfig;

	protected JobBase(final Config appConfig) {
		localConfig = new Config(appConfig);
	}

	@Override
	public final Config getConfig() {
		return localConfig;
	}
	
	@Override
	public void run() {
		final JobConfig jobConfig = localConfig.getLoadConfig().getJobConfig();
		try {
			String jobName = jobConfig.getName();
			if(jobName == null) {
				jobName = ThreadContext.get(KEY_JOB_NAME);
				if(jobName == null) {
					LOG.fatal(Markers.ERR, "Job name is not set");
				} else {
					jobConfig.setName(jobName);
				}
			} else {
				ThreadContext.put(KEY_JOB_NAME, jobName);
			}
		} catch(final Throwable t) {
			LogUtil.exception(LOG, Level.ERROR, t, "Unexpected failure");
		}
	}
}
