package com.emc.mongoose.run.scenario;

import com.emc.mongoose.ui.config.Config;
import static com.emc.mongoose.ui.config.Config.LoadConfig.JobConfig;
import static com.emc.mongoose.common.Constants.KEY_JOB_NAME;

import org.apache.logging.log4j.ThreadContext;

/**
 Created by kurila on 08.04.16.
 */
public abstract class JobBase
implements Job {

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
		String jobName = jobConfig.getName();
		if(jobName == null) {
			jobName = ThreadContext.get(KEY_JOB_NAME);
			jobConfig.setName(jobName);
		} else {
			ThreadContext.put(KEY_JOB_NAME, jobName);
		}
		if(jobName == null) {
			throw new IllegalStateException("Job name is not set");
		}
	}
}
