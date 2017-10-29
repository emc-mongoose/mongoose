package com.emc.mongoose.run;

import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.run.scenario.Scenario;
import com.emc.mongoose.run.scenario.ScenarioParseException;
import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.IllegalArgumentNameException;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static com.emc.mongoose.run.scenario.Scenario.FNAME_DEFAULT_SCENARIO;
import static com.emc.mongoose.ui.cli.CliArgParser.formatCliArgsList;
import static com.emc.mongoose.ui.cli.CliArgParser.getAllCliArgs;

import org.apache.logging.log4j.CloseableThreadContext;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 Created by kurila on 11.07.16.
 */
public class Main {

	@SuppressWarnings("unchecked")
	public static void main(final String... args)
	throws Exception {
		
		LogUtil.init();

		final Config config = Config.loadDefaults();
		if(config == null) {
			throw new AssertionError();
		}

		try {
			config.apply(
				CliArgParser.parseArgs(config.getAliasingConfig(), args),
				"none-" + LogUtil.getDateTimeStamp()
			);
		} catch(final IllegalArgumentNameException e) {
			Loggers.ERR.fatal(
				"Invalid argument: \"{}\"\nThe list of all possible args:\n{}", e.getMessage(),
				formatCliArgsList(getAllCliArgs())
			);
			return;
		}

		final String scenarioValue = config.getTestConfig().getScenarioConfig().getFile();
		final Path scenarioPath;

		try(
			final CloseableThreadContext.Instance ctx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, config.getTestConfig().getStepConfig().getId())
				.put(KEY_CLASS_NAME, Main.class.getSimpleName())
		) {
			Arrays.stream(args).forEach(Loggers.CLI::info);
			Loggers.CONFIG.info(config.toString());

			if(scenarioValue != null && !scenarioValue.isEmpty()) {
				scenarioPath = Paths.get(scenarioValue);
			} else {
				scenarioPath = Paths.get(getBaseDir(), DIR_SCENARIO, FNAME_DEFAULT_SCENARIO);
			}

			Files.lines(scenarioPath).forEach(Loggers.SCENARIO::info);
		}

		try(final Scenario scenario = new JsonScenario(config, scenarioPath.toFile())) {
			scenario.run();
		} catch(final ScenarioParseException e) {
			Loggers.ERR.fatal(
				"Failed to parse the scenario \"{}\": {}", scenarioPath, e.getMessage()
			);
		} catch(final FileNotFoundException e) {
			Loggers.ERR.fatal("Scenario file \"{}\" not found", scenarioPath);
		} catch(final Throwable t) {
			t.printStackTrace(System.err);
		}
	}
}
