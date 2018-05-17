package com.emc.mongoose.scenario.step;

import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.impl.BasicConfig;
import org.apache.logging.log4j.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ServiceLoader;

/**
 Created by andrey on 19.09.17.
 */
public interface ScriptEngineUtil {

	/**
	 Tries to instantiate the script engine for the given script file
	 @param scenarioPath the path to the script
	 @return the script engine resolved either <code>null</code>
	 */
	static ScriptEngine resolve(final Path scenarioPath, final ClassLoader clsLoader) {

		ScriptEngine se = null;

		// init the available external script engines
		final ScriptEngineManager sem = new ScriptEngineManager(clsLoader);

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
			for(final ScriptEngineFactory sef: sem.getEngineFactories()) {
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
	static void registerStepTypes(
		final ScriptEngine se, final ClassLoader clsLoader, final Config config
	) {

		final ServiceLoader<LoadStepFactory<? extends LoadStep>>
			loader = ServiceLoader.load((Class) LoadStepFactory.class, clsLoader);

		for(final LoadStepFactory<? extends LoadStep> factory: loader) {
			se.put(factory.getTypeName(), factory.create(config, clsLoader, null));
		}

		// Expose the additional/shortcut step types to the given script engine using the given
		// configuration
		final String baseLoadStepTypeName = "Load";
		LoadStepFactory<? extends LoadStep> baseLoadStepFactory = null;
		for(final LoadStepFactory<? extends LoadStep> factory: loader) {
			if(baseLoadStepTypeName.equals(factory.getTypeName())) {
				baseLoadStepFactory = factory;
				break;
			}
		}
		if(baseLoadStepFactory == null) {
			Loggers.ERR.warn(
				"Basic load step type \"{}\" implementation not found", baseLoadStepTypeName
			);
			return;
		}

		Config specificConfig;

		specificConfig = new BasicConfig(config);
		specificConfig.val("output-metrics-average-period", 0);
		specificConfig.val("output-metrics-average-persist", false);
		specificConfig.val("output-metrics-summary-perfDbResultsFile", false);
		specificConfig.val("output-metrics-summary-persist", false);
		se.put("PreconditionLoad", baseLoadStepFactory.create(specificConfig, clsLoader, null));

		for(final IoType ioType : IoType.values()) {
			specificConfig = new BasicConfig(config);
			final String ioTypeName = ioType.name().toLowerCase();
			specificConfig.val("load-type", ioTypeName);
			final String stepName = ioTypeName.substring(0, 1).toUpperCase()
				+ ioTypeName.substring(1) + "Load";
			se.put(stepName, baseLoadStepFactory.create(specificConfig, clsLoader, null));
		}

		specificConfig = new BasicConfig(config);
		specificConfig.val("load-type", IoType.READ.name().toLowerCase());
		specificConfig.val("item-data-verify", true);
		se.put("ReadVerifyLoad", baseLoadStepFactory.create(specificConfig, clsLoader, null));

		specificConfig = new BasicConfig(config);
		specificConfig.val("load-type", IoType.READ.name().toLowerCase());
		specificConfig.val("item-data-ranges-random", 1);
		se.put("ReadRandomRangeLoad", baseLoadStepFactory.create(specificConfig, clsLoader, null));

		specificConfig = new BasicConfig(config);
		specificConfig.val("load-type", IoType.READ.name().toLowerCase());
		specificConfig.val("item-data-verify", true);
		specificConfig.val("item-data-ranges-random", 1);
		se.put(
			"ReadVerifyRandomRangeLoad", baseLoadStepFactory.create(specificConfig, clsLoader, null)
		);

		specificConfig = new BasicConfig(config);
		specificConfig.val("load-type", IoType.UPDATE.name().toLowerCase());
		specificConfig.val("item-data-ranges-random", 1);
		se.put(
			"UpdateRandomRangeLoad", baseLoadStepFactory.create(specificConfig, clsLoader, null)
		);
	}
}
