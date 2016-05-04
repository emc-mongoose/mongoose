package com.emc.mongoose.run.scenario.runner;
// mongoose-common.jar
import com.emc.mongoose.common.conf.AppConfig;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.run.scenario.engine.JsonScenario;
import com.emc.mongoose.run.scenario.engine.Scenario;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.File;
import java.io.IOException;
/**
 Created by kurila on 12.05.14.
 A scenario runner utility class.
 */
public final class ScenarioRunner
implements Runnable {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	private Scenario scenario = null;
	//
	public ScenarioRunner(final Scenario scenario) {
		this.scenario = scenario;
	}
	//
	public ScenarioRunner(final AppConfig appConfig) {
		final String runFile = appConfig.getRunFile();
		try {
			if(runFile == null || runFile.isEmpty()) {
				LOG.info(Markers.MSG, "Using the scenario from the standard input...");
				scenario = new JsonScenario(appConfig, System.in);
			} else {
				LOG.info(Markers.MSG, "Using the scenario from the file \"{}\"", runFile);
				scenario = new JsonScenario(appConfig, new File(runFile));
			}

		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Scenario reading failure");
		} catch(final CloneNotSupportedException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Configuration spawning failure");
		}
	}
	//
	public void run() {
		if(scenario != null) {
			try {
				scenario.run();
			} catch(final Throwable t) {
				LogUtil.exception(LOG, Level.ERROR, t, "Scenario execution failure");
			}
		}
	}
}
