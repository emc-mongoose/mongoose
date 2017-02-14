package com.emc.mongoose.run;

import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.run.scenario.Scenario;
import com.emc.mongoose.run.scenario.ScenarioParseException;
import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.reader.jackson.ConfigParser;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static com.emc.mongoose.run.scenario.Scenario.FNAME_DEFAULT_SCENARIO;

/**
 Created by kurila on 11.07.16.
 */
public class Main {

	@SuppressWarnings("unchecked")
	public static void main(final String... args)
	throws Exception {
		
		LogUtil.init();
		final Logger log = LogManager.getLogger();

		final Config config = ConfigParser.loadDefaultConfig();
		if(config == null) {
			throw new AssertionError();
		}
		config.apply(CliArgParser.parseArgs(config.getAliasingConfig(), args));

		final String scenarioValue = config.getScenarioConfig().getFile();
		final Path scenarioPath;
		if(scenarioValue != null && !scenarioValue.isEmpty()) {
			scenarioPath = Paths.get(scenarioValue);
		} else {
			scenarioPath = Paths.get(getBaseDir(), DIR_SCENARIO, FNAME_DEFAULT_SCENARIO);
		}
		
		try(final Scenario scenario = new JsonScenario(config, scenarioPath.toFile())) {
			scenario.run();
		} catch(final ScenarioParseException e) {
			log.fatal(
				Markers.ERR, "Failed to parse the scenario \"{}\": {}", scenarioPath, e.getMessage()
			);
		} catch(final FileNotFoundException e) {
			log.fatal(Markers.ERR, "Scenario file \"{}\" not found", scenarioPath);
		} catch(final Throwable t) {
			t.printStackTrace(System.err);
		}
	}
}
