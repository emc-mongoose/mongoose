package com.emc.mongoose.run.scenario.jsr223;

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
import org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 Created by andrey on 14.09.17.
 */
public final class Main {

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

		try(
			final Instance ctx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, config.getTestConfig().getStepConfig().getId())
				.put(KEY_CLASS_NAME, Main.class.getSimpleName())
		) {
			Arrays.stream(args).forEach(Loggers.CLI::info);
			Loggers.CONFIG.info(config.toString());

			final Path scenarioPath;
			final String scenarioValue = config.getTestConfig().getScenarioConfig().getFile();
			if(scenarioValue != null && !scenarioValue.isEmpty()) {
				scenarioPath = Paths.get(scenarioValue);
			} else {
				scenarioPath = Paths.get(getBaseDir(), DIR_SCENARIO, FNAME_DEFAULT_SCENARIO);
			}

			final StringBuilder strb = new StringBuilder();
			Files.lines(scenarioPath).forEach(strb::append);
			final String scenarioText = strb.toString();
			Loggers.SCENARIO.log(Level.INFO, scenarioText);

			final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
			final ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("nashorn");

			scriptEngine.put("command", new CommandStep(config));
			scriptEngine.put("load", new LoadStep(config));
			scriptEngine.put("parallel", new ParallelStep(config));

			final Object scenario = scriptEngine.eval(scenarioText);
			scriptEngine.put("scenario", scenario);
			scriptEngine.eval("scenario.run()");
		}
	}
}
