package com.emc.mongoose.scenario.step;

import com.emc.mongoose.model.env.Extensions;
import com.emc.mongoose.config.Config;
import com.emc.mongoose.config.IllegalArgumentNameException;
import com.emc.mongoose.config.test.scenario.ScenarioConfig;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import static com.emc.mongoose.Constants.DIR_EXAMPLE_SCENARIO;
import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.Constants.KEY_TEST_STEP_ID;
import static com.emc.mongoose.model.env.PathUtil.BASE_DIR;
import static com.emc.mongoose.scenario.step.Constants.ATTR_CONFIG;
import static com.emc.mongoose.cli.CliArgParser.formatCliArgsList;
import static com.emc.mongoose.cli.CliArgParser.getAllCliArgs;
import static com.emc.mongoose.cli.CliArgParser.parseArgs;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import static javax.script.ScriptContext.ENGINE_SCOPE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ServiceLoader;

/**
 Created by andrey on 14.09.17.
 */
public final class Main {

	public static void main(final String... args)
	throws Exception {

//		LogUtil.init();

		final Config config = Config.loadDefaults();
		if(config == null) {
			throw new AssertionError();
		}

		try {
			config.apply(
				parseArgs(config.getAliasingConfig(), args), "none-" + LogUtil.getDateTimeStamp()
			);
		} catch(final IllegalArgumentNameException e) {
			System.err.println(
				"Invalid argument: \"" + e.getMessage() + "\"\nThe list of all possible args:\n"
					+ formatCliArgsList(getAllCliArgs())
			);
			return;
		}

		try(
			final CloseableThreadContext.Instance ctx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, config.getTestConfig().getStepConfig().getId())
				.put(KEY_CLASS_NAME, Main.class.getSimpleName())
		) {
			final ClassLoader clsLoader = Extensions.CLS_LOADER;

			Arrays.stream(args).forEach(Loggers.CLI::info);
			Loggers.CONFIG.info(config.toString());

			// get the scenario file/path
			final Path scenarioPath;
			final ScenarioConfig scenarioConfig = config.getTestConfig().getScenarioConfig();
			final String scenarioFile = scenarioConfig.getFile();
			if(scenarioFile != null && !scenarioFile.isEmpty()) {
				scenarioPath = Paths.get(scenarioFile);
			} else {
				scenarioPath = Paths.get(BASE_DIR, DIR_EXAMPLE_SCENARIO, "js", "default.js");
			}

			final StringBuilder strb = new StringBuilder();
			Files
				.lines(scenarioPath)
				.forEach(line -> strb.append(line).append(System.lineSeparator()));
			final String scenarioText = strb.toString();
			Loggers.SCENARIO.log(Level.INFO, scenarioText);

			final ScriptEngine scriptEngine = ScriptEngineUtil.resolve(scenarioPath, clsLoader);
			if(scriptEngine == null) {
				Loggers.ERR.fatal(
					"Failed to resolve the scenario engine for the file \"{}\"", scenarioPath
				);
			} else {

				Loggers.MSG.info(
					"Using the \"{}\" scenario engine", scriptEngine.getFactory().getEngineName()
				);

				// expose the environment values
				System.getenv().forEach(scriptEngine::put);
				// expose the loaded configuration
				scriptEngine.getContext().setAttribute(ATTR_CONFIG, config, ENGINE_SCOPE);
				// expose the step types
				final ServiceLoader<LoadStepFactory<? extends LoadStep>>
					loader = ServiceLoader.load((Class) LoadStepFactory.class, clsLoader);
				ScriptEngineUtil.registerStepBasicTypes(scriptEngine, loader, config);
				ScriptEngineUtil.registerStepShortcutTypes(scriptEngine, loader, config);
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
