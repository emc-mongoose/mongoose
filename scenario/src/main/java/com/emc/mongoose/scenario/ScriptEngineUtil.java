package com.emc.mongoose.scenario;

import com.emc.mongoose.api.common.env.Extensions;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.scenario.step.ChainLoadStep;
import com.emc.mongoose.scenario.step.CommandStep;
import com.emc.mongoose.scenario.step.LoadStep;
import com.emc.mongoose.scenario.step.ParallelStep;
import com.emc.mongoose.scenario.step.WeightedLoadStep;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
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
		final ScriptEngineManager sem = new ScriptEngineManager(Extensions.CLS_LOADER);

		// 1st try to determine the scenario type by the scenario file extension
		final String scenarioFileName = scenarioPath.getFileName().toString();
		int dotPos = scenarioFileName.lastIndexOf('.');
		if(dotPos > 0) {
			final String scenarioFileExt = scenarioFileName.substring(dotPos + 1);
			se = sem.getEngineByExtension(scenarioFileExt);
		}

		if(se == null) {
			// 2nd: try to determine the scenario MIME type
			try {
				final String scenarioMimeType = Files.probeContentType(scenarioPath);
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
			for(final ScriptEngineFactory sef : sem.getEngineFactories()) {
				Loggers.MSG.info(
					"\nEngine name: {}\n\tLanguage: {}\n\tFile extensions: {}\n\tMIME types: {}",
					sef.getEngineName(), sef.getLanguageName(),
					Arrays.toString(sef.getExtensions().toArray()),
					Arrays.toString(sef.getMimeTypes().toArray())
				);
			}
			// 3rd: consider the scenario file a Javascript file
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
		se.put("Command", new CommandStep(config));
		se.put("ChainLoad", new ChainLoadStep(config));
		se.put("Load", new LoadStep(config));
		se.put("Parallel", new ParallelStep(config));
		se.put("WeightedLoad", new WeightedLoadStep(config));
	}

	/**
	 Expose the additional/shortcut step types to the given script engine using the given
	 configuration
	 @param se the script engine
	 @param config the configuration
	 */
	static void registerStepShortcutTypes(final ScriptEngine se, final Config config) {

		Config specificConfig;

		specificConfig = new Config(config);
		specificConfig.getOutputConfig().getMetricsConfig().getAverageConfig().setPeriod(0);
		specificConfig.getOutputConfig().getMetricsConfig().getAverageConfig().setPersist(false);
		specificConfig.getOutputConfig().getMetricsConfig().getSummaryConfig().setPerfDbResultsFile(false);
		specificConfig.getOutputConfig().getMetricsConfig().getSummaryConfig().setPersist(false);
		specificConfig.getOutputConfig().getMetricsConfig().getTraceConfig().setPersist(false);
		se.put("PreconditionLoad", new LoadStep(specificConfig));

		for(final IoType ioType : IoType.values()) {
			specificConfig = new Config(config);
			final String ioTypeName = ioType.name().toLowerCase();
			specificConfig.getLoadConfig().setType(ioTypeName);
			final String stepName = ioTypeName.substring(0, 1).toUpperCase()
				+ ioTypeName.substring(1) + "Load";
			se.put(stepName, new LoadStep(specificConfig));
		}

		specificConfig = new Config(config);
		specificConfig.getLoadConfig().setType(IoType.READ.name().toLowerCase());
		specificConfig.getItemConfig().getDataConfig().setVerify(true);
		se.put("ReadVerifyLoad", new LoadStep(specificConfig));

		specificConfig = new Config(config);
		specificConfig.getLoadConfig().setType(IoType.READ.name().toLowerCase());
		specificConfig.getItemConfig().getDataConfig().getRangesConfig().setRandom(1);
		se.put("ReadRandomRangeLoad", new LoadStep(specificConfig));

		specificConfig = new Config(config);
		specificConfig.getLoadConfig().setType(IoType.READ.name().toLowerCase());
		specificConfig.getItemConfig().getDataConfig().setVerify(true);
		specificConfig.getItemConfig().getDataConfig().getRangesConfig().setRandom(1);
		se.put("ReadVerifyRandomRangeLoad", new LoadStep(specificConfig));

		specificConfig = new Config(config);
		specificConfig.getLoadConfig().setType(IoType.UPDATE.name().toLowerCase());
		specificConfig.getItemConfig().getDataConfig().getRangesConfig().setRandom(1);
		se.put("UpdateRandomRangeLoad", new LoadStep(specificConfig));
	}

}
