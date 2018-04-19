package com.emc.mongoose.scenario;

import com.emc.mongoose.api.common.env.Extensions;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.scenario.sna.LinearLoadStep;
import com.emc.mongoose.scenario.sna.WeightedLoadStep;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 Created by andrey on 19.09.17.
 */
public interface ScriptEngineUtil {

	/**
	 Tries to instantiate the script engine for the given script file
	 @param scenarioPath the path to the script
	 @return the script engine resolved either <code>null</code>
	 */
	static ScriptEngine resolve(final Path scenarioPath) {

		ScriptEngine se = null;

		// init the available external script engines
		final var sem = new ScriptEngineManager(Extensions.CLS_LOADER);

		// 1st try to determine the scenario type by the scenario file extension
		final var scenarioFileName = scenarioPath.getFileName().toString();
		var dotPos = scenarioFileName.lastIndexOf('.');
		if(dotPos > 0) {
			final var scenarioFileExt = scenarioFileName.substring(dotPos + 1);
			se = sem.getEngineByExtension(scenarioFileExt);
		}

		if(se == null) {
			// 2nd: try to determine the scenario MIME type
			try {
				final var scenarioMimeType = Files.probeContentType(scenarioPath);
				if(scenarioMimeType != null) {
					se = sem.getEngineByMimeType(scenarioMimeType);
				}
			} catch(final IOException e) {
				LogUtil.exception(
					Level.WARN, e,
					"Failed to determine the content type for the scenario file \"{}\"",
					scenarioPath
				);
			}
		}

		if(se == null) {
			Loggers.MSG.info(
				"Unable to resolve the scenario engine for the scenario file \"{}\", "
					+ "available scenario engines list follows:", scenarioPath
			);
			for(final var sef : sem.getEngineFactories()) {
				Loggers.MSG.info(
					"\nEngine name: {}\n\tLanguage: {}\n\tFile extensions: {}\n\tMIME types: {}",
					sef.getEngineName(), sef.getLanguageName(),
					Arrays.toString(sef.getExtensions().toArray()),
					Arrays.toString(sef.getMimeTypes().toArray())
				);
			}
			// 3rd: treat the scenario file as a Javascript file
			se = sem.getEngineByName("js");
		}

		return se;
	}

	/**
	 Expose the step types to the given script engine using the given configuration
	 @param se the script engine
	 @param config the configuration
	 */
	static void registerStepBasicTypes(final ScriptEngine se, final Config config) {
		se.put(LinearLoadStep.TYPE, new LinearLoadStep(config));
		se.put(WeightedLoadStep.TYPE, new WeightedLoadStep(config));
	}

	/**
	 Expose the additional/shortcut step types to the given script engine using the given
	 configuration
	 @param se the script engine
	 @param config the configuration
	 */
	static void registerStepShortcutTypes(final ScriptEngine se, final Config config) {

		var specificConfig = new Config(config);

		specificConfig.getOutputConfig().getMetricsConfig().getAverageConfig().setPeriod(0);
		specificConfig.getOutputConfig().getMetricsConfig().getAverageConfig().setPersist(false);
		specificConfig
			.getOutputConfig().getMetricsConfig().getSummaryConfig().setPerfDbResultsFile(false);
		specificConfig.getOutputConfig().getMetricsConfig().getSummaryConfig().setPersist(false);
		specificConfig.getOutputConfig().getMetricsConfig().getTraceConfig().setPersist(false);
		se.put("PreconditionLoad", new LinearLoadStep(specificConfig));

		for(final var ioType : IoType.values()) {
			specificConfig = new Config(config);
			final var ioTypeName = ioType.name().toLowerCase();
			specificConfig.getLoadConfig().setType(ioTypeName);
			final var stepName = ioTypeName.substring(0, 1).toUpperCase() +
				ioTypeName.substring(1) + "Load";
			se.put(stepName, new LinearLoadStep(specificConfig));
		}

		specificConfig = new Config(config);
		specificConfig.getLoadConfig().setType(IoType.READ.name().toLowerCase());
		specificConfig.getItemConfig().getDataConfig().setVerify(true);
		se.put("ReadVerifyLoad", new LinearLoadStep(specificConfig));

		specificConfig = new Config(config);
		specificConfig.getLoadConfig().setType(IoType.READ.name().toLowerCase());
		specificConfig.getItemConfig().getDataConfig().getRangesConfig().setRandom(1);
		se.put("ReadRandomRangeLoad", new LinearLoadStep(specificConfig));

		specificConfig = new Config(config);
		specificConfig.getLoadConfig().setType(IoType.READ.name().toLowerCase());
		specificConfig.getItemConfig().getDataConfig().setVerify(true);
		specificConfig.getItemConfig().getDataConfig().getRangesConfig().setRandom(1);
		se.put("ReadVerifyRandomRangeLoad", new LinearLoadStep(specificConfig));

		specificConfig = new Config(config);
		specificConfig.getLoadConfig().setType(IoType.UPDATE.name().toLowerCase());
		specificConfig.getItemConfig().getDataConfig().getRangesConfig().setRandom(1);
		se.put("UpdateRandomRangeLoad", new LinearLoadStep(specificConfig));
	}

}
