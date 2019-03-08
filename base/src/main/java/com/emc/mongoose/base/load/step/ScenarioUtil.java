package com.emc.mongoose.base.load.step;

import static com.emc.mongoose.base.Constants.DIR_EXAMPLE_SCENARIO;
import static javax.script.ScriptContext.ENGINE_SCOPE;

import com.emc.mongoose.base.env.Extension;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;
import com.emc.mongoose.base.metrics.MetricsManager;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.impl.BasicConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import org.apache.logging.log4j.Level;

/** Created by andrey on 19.09.17. */
public interface ScenarioUtil {

	String DEFAULT_LANG = "js";

	static ScriptEngineManager scriptEngineManager(final ClassLoader clsLoader) {
		return new ScriptEngineManager(clsLoader);
	}

	static ScriptEngine defaultScriptEngine(final ScriptEngineManager sem) {
		return sem.getEngineByName(DEFAULT_LANG);
	}

	/**
	 * Tries to instantiate the script engine for the given script file
	 *
	 * @param scenarioPath the path to the script
	 * @return the script engine resolved either <code>null</code>
	 */
	static ScriptEngine scriptEngineByFilePath(final Path scenarioPath, final ClassLoader clsLoader) {

		ScriptEngine se = null;

		// init the available external script engines
		final ScriptEngineManager sem = scriptEngineManager(clsLoader);

		// 1st try to determine the scenario type by the scenario file extension
		final String scenarioFileName = scenarioPath.getFileName().toString();
		final int dotPos = scenarioFileName.lastIndexOf('.');
		if (dotPos > 0) {
			final String scenarioFileExt = scenarioFileName.substring(dotPos + 1);
			se = sem.getEngineByExtension(scenarioFileExt);
		}

		if (se == null) {
			// 2nd: try to determine the scenario MIME type
			try {
				final String scenarioMimeType = Files.probeContentType(scenarioPath);
				if (scenarioMimeType != null) {
					se = sem.getEngineByMimeType(scenarioMimeType);
				}
			} catch (final IOException e) {
				LogUtil.exception(
								Level.WARN,
								e,
								"Failed to determine the content type for the scenario file \"{}\"",
								scenarioPath);
			}
		}

		if (se == null) {
			Loggers.MSG.info(
							"Unable to resolve the scenario engine for the scenario file \"{}\", "
											+ "available scenario engines list follows:",
							scenarioPath);
			for (final ScriptEngineFactory sef : sem.getEngineFactories()) {
				Loggers.MSG.info(
								"\nEngine name: {}\n\tLanguage: {}\n\tFile extensions: {}\n\tMIME types: {}",
								sef.getEngineName(),
								sef.getLanguageName(),
								Arrays.toString(sef.getExtensions().toArray()),
								Arrays.toString(sef.getMimeTypes().toArray()));
			}
			// 3rd: treat the scenario file as a Javascript file
			se = defaultScriptEngine(sem);
		}

		return se;
	}

	static ScriptEngine scriptEngineByDefault(final ClassLoader clsLoader) {
		return defaultScriptEngine(scriptEngineManager(clsLoader));
	}

	static void registerScenarioDefaults(final ScriptEngine scriptEngine, final Config defaults) {
		scriptEngine.getContext().setAttribute(Constants.ATTR_CONFIG, defaults, ENGINE_SCOPE);
	}

	/**
	 * Expose the step types to the given script engine using the given configuration
	 *
	 * @param se the script engine
	 * @param config the configuration
	 */
	static void registerStepTypes(
					final ScriptEngine se,
					final List<Extension> extensions,
					final Config config,
					final MetricsManager metricsMgr) {
		final List<LoadStepFactory> loadStepFactories = extensions.stream()
						.filter(ext -> ext instanceof LoadStepFactory)
						.map(ext -> (LoadStepFactory) ext)
						.collect(Collectors.toList());
		loadStepFactories.forEach(
						factory -> se.put(factory.id(), factory.createClient(config, extensions, metricsMgr)));
		loadStepFactories.stream()
						.filter(factory -> "Load".equals(factory.id()))
						.findFirst()
						.ifPresent(
										factory -> registerAdditionalStepTypes(se, extensions, config, metricsMgr, factory));
	}

	static void registerAdditionalStepTypes(
					final ScriptEngine se,
					final List<Extension> extensions,
					final Config config,
					final MetricsManager metricsMgr,
					final LoadStepFactory baseLoadStepFactory) {

		Config specificConfig;

		specificConfig = new BasicConfig(config);
		specificConfig.val("output-metrics-average-persist", false);
		specificConfig.val("output-metrics-summary-persist", false);
		specificConfig.val("output-metrics-trace-persist", false);
		se.put(
						"PreconditionLoad",
						baseLoadStepFactory.createClient(specificConfig, extensions, metricsMgr));

		for (final OpType opType : OpType.values()) {
			specificConfig = new BasicConfig(config);
			final String ioTypeName = opType.name().toLowerCase();
			specificConfig.val("load-op-type", ioTypeName);
			final String stepName = ioTypeName.substring(0, 1).toUpperCase() + ioTypeName.substring(1) + "Load";
			se.put(stepName, baseLoadStepFactory.createClient(specificConfig, extensions, metricsMgr));
		}

		specificConfig = new BasicConfig(config);
		specificConfig.val("load-op-type", OpType.READ.name().toLowerCase());
		specificConfig.val("item-data-verify", true);
		se.put(
						"ReadVerifyLoad", baseLoadStepFactory.createClient(specificConfig, extensions, metricsMgr));

		specificConfig = new BasicConfig(config);
		specificConfig.val("load-op-type", OpType.READ.name().toLowerCase());
		specificConfig.val("item-data-ranges-random", 1);
		se.put(
						"ReadRandomRangeLoad",
						baseLoadStepFactory.createClient(specificConfig, extensions, metricsMgr));

		specificConfig = new BasicConfig(config);
		specificConfig.val("load-op-type", OpType.READ.name().toLowerCase());
		specificConfig.val("item-data-verify", true);
		specificConfig.val("item-data-ranges-random", 1);
		se.put(
						"ReadVerifyRandomRangeLoad",
						baseLoadStepFactory.createClient(specificConfig, extensions, metricsMgr));

		specificConfig = new BasicConfig(config);
		specificConfig.val("load-op-type", OpType.UPDATE.name().toLowerCase());
		specificConfig.val("item-data-ranges-random", 1);
		se.put(
						"UpdateRandomRangeLoad",
						baseLoadStepFactory.createClient(specificConfig, extensions, metricsMgr));
	}

	static void configure(
					final ScriptEngine se,
					final List<Extension> extensions,
					final Config config,
					final MetricsManager metricsMgr) {
		registerScenarioDefaults(se, config);
		registerStepTypes(se, extensions, config, metricsMgr);
	}

	static Path defaultScenarioPath(final Path appHomePath) {
		return Paths.get(appHomePath.toString(), DIR_EXAMPLE_SCENARIO, "js", "default.js");
	}

	static String defaultScenario(final Path appHomePath) {
		final Path scenarioPath = defaultScenarioPath(appHomePath);
		final StringBuilder strb = new StringBuilder();
		try {
			Files.lines(scenarioPath).forEach(line -> strb.append(line).append(System.lineSeparator()));
		} catch (final IOException e) {
			throw new AssertionError("Failed to read the scenario file \"" + scenarioPath + "\"");
		}
		return strb.toString();
	}
}
