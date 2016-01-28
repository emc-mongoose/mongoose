package com.emc.mongoose.run.scenario.runner;
// mongoose-common.jar
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
//
import com.emc.mongoose.common.log.Markers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 Created by kurila on 12.05.14.
 A scenario runner utility class.
 */
public final class ScenarioRunner
implements Runnable {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	public void run() {
		final AppConfig localAppConfig = BasicConfig.THREAD_CONTEXT.get();
		if (localAppConfig != null) {
			// TODO
			LOG.info(Markers.MSG, "Scenario end");
		}
	}
}
