package com.emc.mongoose.run;

import com.emc.mongoose.common.exception.IoFireball;
import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.Config;
import static com.emc.mongoose.common.Constants.KEY_JOB_NAME;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.JobConfig;
import com.emc.mongoose.ui.config.reader.jackson.ConfigParser;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import com.emc.mongoose.run.scenario.LoadJob;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

/**
 Created by kurila on 11.07.16.
 */
public class Main {

	static {
		LogUtil.init();
	}

	@SuppressWarnings("unchecked")
	public static void main(final String... args)
	throws IoFireball, UserShootHisFootException {

		final Config config = ConfigParser.loadDefaultConfig();
		if(config == null) {
			throw new UserShootHisFootException("Config is null");
		}
		config.apply(CliArgParser.parseArgs(args));
		
		final LoadConfig loadConfig = config.getLoadConfig();
		final JobConfig jobConfig = loadConfig.getJobConfig();

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
		
		final Logger log = LogManager.getLogger();
		log.info(Markers.MSG, "Configuration loaded");

		new LoadJob(config).run();
	}
}
