package com.emc.mongoose.scenario;

import com.emc.mongoose.scenario.json.JsonScriptEngineFactory;
import com.emc.mongoose.scenario.step.ChainLoadStep;
import com.emc.mongoose.scenario.step.CommandStep;
import com.emc.mongoose.scenario.step.LoadStep;
import com.emc.mongoose.scenario.step.ParallelStep;
import com.emc.mongoose.scenario.step.WeightedLoadStep;
import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.IllegalArgumentNameException;
import com.emc.mongoose.ui.config.test.scenario.ScenarioConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.scenario.Scenario.DIR_SCENARIO;
import static com.emc.mongoose.ui.cli.CliArgParser.formatCliArgsList;
import static com.emc.mongoose.ui.cli.CliArgParser.getAllCliArgs;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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

			// init the Mongoose's JSON script engine
			final ScriptEngineManager internalManager = new ScriptEngineManager();
			final ScriptEngineFactory jsonScriptEngineFactory = new JsonScriptEngineFactory(config);
			for(final String langName : JsonScriptEngineFactory.NAMES) {
				internalManager.registerEngineName(langName, jsonScriptEngineFactory);
			}
			internalManager.registerEngineName(
				JsonScriptEngineFactory.ENGINE_NAME, jsonScriptEngineFactory
			);
			for(final String jsonScriptExt : JsonScriptEngineFactory.EXTENSIONS) {
				internalManager.registerEngineExtension(jsonScriptExt, jsonScriptEngineFactory);
			}
			for(final String jsonMimeType : JsonScriptEngineFactory.MIME_TYPES) {
				internalManager.registerEngineMimeType(jsonMimeType, jsonScriptEngineFactory);
			}

			// init the configured script engines
			final ScriptEngineManager externalManager;
			final ScenarioConfig scenarioConfig = config.getTestConfig().getScenarioConfig();
			final List<String> scriptEngines = scenarioConfig.getEngines();
			if(scriptEngines != null) {
				final int n = scriptEngines.size();
				if(n > 0) {
					final URL[] scriptEngineJarUrls = new URL[n];
					for(int i = 0; i < n; i ++) {
						try {
							scriptEngineJarUrls[i] = new File(scriptEngines.get(i)).toURI().toURL();
						} catch(final MalformedURLException e) {
							Loggers.ERR.warn(
								"Invalid script engine implementation path: {}",
								scriptEngines.get(i)
							);
						}
					}
					final URLClassLoader clsLoader = new URLClassLoader(scriptEngineJarUrls);
					externalManager = new ScriptEngineManager(clsLoader);
				} else {
					externalManager = null;
				}
			} else {
				externalManager = null;
			}

			// get the scenario file/path
			final Path scenarioPath;
			final String scenarioFile = scenarioConfig.getFile();
			if(scenarioFile != null && !scenarioFile.isEmpty()) {
				scenarioPath = Paths.get(scenarioFile);
			} else {
				scenarioPath = Paths.get(getBaseDir(), DIR_SCENARIO, "js", "default.js");
			}

			final StringBuilder strb = new StringBuilder();
			Files.lines(scenarioPath).forEach(strb::append);
			final String scenarioText = strb.toString();
			Loggers.SCENARIO.log(Level.INFO, scenarioText);

			ScriptEngine scriptEngine;

			final String scenarioMimeType = Files.probeContentType(scenarioPath);
			if(scenarioMimeType != null) {
				scriptEngine = internalManager.getEngineByMimeType(scenarioMimeType);
				if(scriptEngine == null && externalManager != null) {
					scriptEngine = externalManager.getEngineByMimeType(scenarioMimeType);
				}
				if(scriptEngine == null) {
					Loggers.ERR.fatal(
						"Failed to resolve the script engine for the MIME type \"{}\"",
						scenarioMimeType
					);
					return;
				}
			} else {
				String scenarioFileExt = scenarioPath.toString();
				int dotPos = scenarioFileExt.lastIndexOf('.');
				if(dotPos > 0) {
					scenarioFileExt = scenarioFileExt.substring(dotPos + 1);
					scriptEngine = internalManager.getEngineByExtension(scenarioFileExt);
					if(scriptEngine == null && externalManager != null) {
						scriptEngine = externalManager.getEngineByExtension(scenarioFileExt);
					}
					if(scriptEngine == null) {
						Loggers.ERR.fatal(
							"Failed to resolve the script engine for the file extension: \"{}\"",
							scenarioFileExt
						);
						return;
					}
				} else {
					Loggers.ERR.warn(
						"Failed to determine the scenario type for the file \"{}\"", scenarioPath
					);
					scriptEngine = internalManager.getEngineByName("js");
					if(scriptEngine == null && externalManager != null) {
						scriptEngine = externalManager.getEngineByName("js");
					}
					if(scriptEngine == null) {
						Loggers.ERR.fatal(
							"Failed to resolve the default scenario engine", scenarioFileExt
						);
						return;
					}
				}
			}

			Loggers.MSG.info(
				"Using the \"{}\" scenario engine", scriptEngine.getFactory().getEngineName()
			);

			// expose the environment values
			final Map<String, String> env = System.getenv();
			for(final String envKey : env.keySet()) {
				scriptEngine.put(envKey, env.get(envKey));
			}

			// expose the step types
			scriptEngine.put("command", new CommandStep(config));
			scriptEngine.put("chain", new ChainLoadStep(config));
			scriptEngine.put("load", new LoadStep(config));
			scriptEngine.put("parallel", new ParallelStep(config));
			scriptEngine.put("weighted", new WeightedLoadStep(config));

			// go
			scriptEngine.eval(scenarioText);
		}
	}
}
