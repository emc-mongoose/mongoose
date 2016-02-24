package com.emc.mongoose.run.scenario.runner;
// mongoose-common.jar
import com.emc.mongoose.common.conf.AppConfig;
//
import com.emc.mongoose.run.scenario.engine.JsonScenario;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.File;
/**
 Created by kurila on 12.05.14.
 A scenario runner utility class.
 */
public final class ScenarioRunner
implements Runnable {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	private final AppConfig appConfig;
	//
	public ScenarioRunner(final AppConfig appConfig) {
		this.appConfig = appConfig;
	}
	//
	public void run() {
		final String runFile = appConfig.getRunFile();
		new JsonScenario(new File(runFile)).run();
	}
}
