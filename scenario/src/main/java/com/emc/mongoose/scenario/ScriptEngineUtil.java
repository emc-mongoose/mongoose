package com.emc.mongoose.scenario;

import com.emc.mongoose.api.common.env.Extensions;
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

	static ScriptEngine resolve(final Path scenarioPath) {

		ScriptEngine se = null;

		// init the available external script engines
		final ScriptEngineManager sem = new ScriptEngineManager(Extensions.CLS_LOADER);

		// 1st try to determine the scenario type by the scenario file extension
		String scenarioFileExt = scenarioPath.toString();
		int dotPos = scenarioFileExt.lastIndexOf('.');
		if(dotPos > 0) {
			scenarioFileExt = scenarioFileExt.substring(dotPos + 1);
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

}
