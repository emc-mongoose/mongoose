package com.emc.mongoose.scenario;

import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.IllegalArgumentNameException;
import com.emc.mongoose.ui.config.test.scenario.ScenarioConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.scenario.Constants.ATTR_CONFIG;
import static com.emc.mongoose.ui.cli.CliArgParser.formatCliArgsList;
import static com.emc.mongoose.ui.cli.CliArgParser.getAllCliArgs;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import static javax.script.ScriptContext.ENGINE_SCOPE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

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

			// get the scenario file/path
			final Path scenarioPath;
			final ScenarioConfig scenarioConfig = config.getTestConfig().getScenarioConfig();
			final String scenarioFile = scenarioConfig.getFile();
			if(scenarioFile != null && !scenarioFile.isEmpty()) {
				scenarioPath = Paths.get(scenarioFile);
			} else {
				scenarioPath = Paths.get(getBaseDir(), DIR_EXAMPLE_SCENARIO, "js", "default.js");
			}

			final StringBuilder strb = new StringBuilder();
			Files
				.lines(scenarioPath)
				.forEach(line -> strb.append(line).append(System.lineSeparator()));
			final String scenarioText = strb.toString();
			Loggers.SCENARIO.log(Level.INFO, scenarioText);

			final ScriptEngine scriptEngine = ScriptEngineUtil.resolve(scenarioPath);
			if(scriptEngine == null) {
				Loggers.ERR.fatal(
					"Failed to resolve the scenario engine for the file \"{}\"", scenarioPath
				);
			} else {

				Loggers.MSG.info(
					"Using the \"{}\" scenario engine", scriptEngine.getFactory().getEngineName()
				);

				// expose the environment values
				final Map<String, String> env = System.getenv();
				for(final String envKey : env.keySet()) {
					scriptEngine.put(envKey, env.get(envKey));
				}

				// expose the loaded configuration
				scriptEngine.getContext().setAttribute(ATTR_CONFIG, config, ENGINE_SCOPE);
				// expose the step types
				ScriptEngineUtil.registerStepBasicTypes(scriptEngine, config);
				ScriptEngineUtil.registerStepShortcutTypes(scriptEngine, config);
				// go
				try {
					scriptEngine.eval(scenarioText);
				} catch(final ScriptException e) {
					LogUtil.exception(
						Level.ERROR, e,
						"\nScenario failed @ file \"{}\", line #{}, column #{}:\n{}",
						scenarioPath, e.getLineNumber(), e.getColumnNumber(), e.getMessage()
					);
				}
			}
		}
	}
}
