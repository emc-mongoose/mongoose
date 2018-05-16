package com.emc.mongoose;

import com.emc.mongoose.cli.CliArgParser;
import com.emc.mongoose.cli.CliArgUtil;
import com.emc.mongoose.config.AliasingUtil;
import com.emc.mongoose.config.IllegalArgumentNameException;
import com.emc.mongoose.config.ConfigUtil;
import com.emc.mongoose.config.scenario.ScenarioConfig;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.model.env.Extensions;

import static com.emc.mongoose.Constants.APP_NAME;
import static com.emc.mongoose.Constants.DIR_EXAMPLE_SCENARIO;
import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.Constants.KEY_STEP_ID;
import static com.emc.mongoose.Constants.PATH_DEFAULTS;
import static com.emc.mongoose.cli.CliArgParser.ARG_PREFIX;
import static com.emc.mongoose.cli.CliArgParser.formatCliArgsList;
import static com.emc.mongoose.cli.CliArgParser.getAllCliArgs;

import com.emc.mongoose.model.svc.Service;
import com.emc.mongoose.scenario.step.ScriptEngineUtil;
import com.emc.mongoose.scenario.step.node.BasicFileManagerService;
import com.emc.mongoose.scenario.step.node.BasicLoadStepManagerService;
import static com.emc.mongoose.scenario.step.Constants.ATTR_CONFIG;

import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import org.apache.logging.log4j.CloseableThreadContext;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import static javax.script.ScriptContext.ENGINE_SCOPE;
import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class Main {

	public static void main(final String... args) {

		final InstallHook installHook = new InstallHook();
		final Path appHomePath = installHook.appHomePath();

		LogUtil.init(appHomePath.toString());
		installHook.run();

		try(final URLClassLoader extClsLoader = Extensions.extClassLoader(appHomePath)) {

			final File defaultsFile = Paths.get(appHomePath.toString(), PATH_DEFAULTS).toFile();
			final Config config;

			try {
				final Map<String, Object> configSchema = SchemaProvider.resolveAndReduce(
					APP_NAME, extClsLoader
				);
				config = ConfigUtil.loadConfig(defaultsFile, configSchema);
			} catch(final Exception e) {
				LogUtil.exception(
					Level.ERROR, e, "Failed to load the defaults from {}", defaultsFile
				);
				return;
			}

			try {
				final Map<String, String> parsedArgs = CliArgUtil.parseArgs(args);
				final List<Map<String, Object>> aliasingConfig = config.listVal("aliasing");
				final Map<String, String> aliasedArgs = AliasingUtil.apply(
					parsedArgs, aliasingConfig
				);
				aliasedArgs.forEach(config::val);
			} catch(final IllegalArgumentNameException e) {
				Loggers.ERR.fatal(
					"Invalid argument: \"{}\"\nThe list of all possible args:\n{}", e.getMessage(),
					formatCliArgsList(getAllCliArgs())
				);
				return;
			}

			/*try {
				config.apply(
					parseArgs(config.getAliasingConfig(), args),
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
					.put(KEY_STEP_ID, config.getScenarioConfig().getStepConfig().getId())
					.put(KEY_CLASS_NAME, Main.class.getSimpleName())
			) {
				Arrays.stream(args).forEach(Loggers.CLI::info);
				Loggers.CONFIG.info(config.toString());
				if(config.getNode()) {
					runNode(config, extClsLoader);
				} else {
					runScenario(config, extClsLoader, appHomePath);
				}
			}*/
		} catch(final IOException e) {
			LogUtil.exception(Level.ERROR, e, "Failed to close the extensions class loader");
		}
	}

	/*private static void runNode(final Config config, final ClassLoader clsLoader)
	throws IOException {
		final int
			listenPort = config.getScenarioConfig().getStepConfig().getNodeConfig().getPort();
		Service inputFileSvc = null;
		Service scenarioStepSvc = null;
		try {
			inputFileSvc = new BasicFileManagerService(listenPort);
			inputFileSvc.start();
			scenarioStepSvc = new BasicLoadStepManagerService(listenPort, clsLoader);
			scenarioStepSvc.start();
			scenarioStepSvc.await();
		} catch(final Throwable cause) {
			cause.printStackTrace(System.err);
		} finally {
			if(inputFileSvc != null) {
				inputFileSvc.close();
			}
			if(scenarioStepSvc != null) {
				scenarioStepSvc.close();
			}
		}
	}

	private static void runScenario(
		final Config config, final ClassLoader clsLoader, final Path appHomePath
	) throws IOException {
		// get the scenario file/path
		final Path scenarioPath;
		final ScenarioConfig scenarioConfig = config.getScenarioConfig();
		final String scenarioFile = scenarioConfig.getFile();
		if(scenarioFile != null && !scenarioFile.isEmpty()) {
			scenarioPath = Paths.get(scenarioFile);
		} else {
			scenarioPath = Paths.get(
				appHomePath.toString(), DIR_EXAMPLE_SCENARIO, "js", "default.js"
			);
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
			ScriptEngineUtil.registerStepTypes(scriptEngine, clsLoader, config);
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
	}*/
}
