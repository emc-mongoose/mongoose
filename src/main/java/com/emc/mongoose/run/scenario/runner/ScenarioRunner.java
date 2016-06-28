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
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.emc.mongoose.common.conf.BasicConfig.getWorkingDir;
import static com.emc.mongoose.run.scenario.engine.Scenario.FNAME_DEFAULT_SCENARIO;
import static com.emc.mongoose.run.scenario.engine.Scenario.DIR_SCENARIO;

/**
 Created by kurila on 12.05.14.
 A scenario runner utility class.
 */
public final class ScenarioRunner
implements Closeable, Runnable {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	private final AppConfig appConfig;
	private Scenario scenario = null;
	//
	public ScenarioRunner(final AppConfig appConfig) {
		this.appConfig = appConfig;
	}
	//
	public ScenarioRunner(final AppConfig appConfig, final Scenario scenario) {
		this.appConfig = appConfig;
		this.scenario = scenario;
	}
	//
	public void run() {
		final boolean useStdInFlag = appConfig.getBoolean(AppConfig.KEY_SCENARIO_FROM_STDIN, false);
		final boolean useWebUiFlag = appConfig.getBoolean(AppConfig.KEY_SCENARIO_FROM_WEBUI, false);
		final String runFileStr = appConfig.getRunFile();
		Path runFilePath;
		if(!useWebUiFlag) {
			try {
				if(useStdInFlag) {
					LOG.info(Markers.MSG, "Using the scenario from the standard input...");
					scenario = new JsonScenario(appConfig, System.in);
				} else {
					if(runFileStr != null && !runFileStr.isEmpty()) {
						runFilePath = Paths.get(runFileStr);
					} else {
						runFilePath = Paths
							.get(getWorkingDir(), DIR_SCENARIO).resolve(FNAME_DEFAULT_SCENARIO);
					}
					LOG.info(
						Markers.MSG, "Using the scenario from the file \"{}\"",
						runFilePath.toString()
					);
					scenario = new JsonScenario(appConfig, runFilePath.toFile());
				}
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Scenario reading failure");
			} catch(final CloneNotSupportedException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Configuration spawning failure");
			}
		}
		//
		if(scenario != null) {
			try {
				scenario.run();
			} catch(final Throwable t) {
				LogUtil.exception(LOG, Level.ERROR, t, "Scenario execution failure");
				t.printStackTrace(System.err);
			}
		}
	}
	//
	@Override
	public final void close()
	throws IOException {
		if(scenario != null) {
			scenario.close();
		}
	}
}
