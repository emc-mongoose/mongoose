package com.emc.mongoose.scenario;

import com.emc.mongoose.api.common.env.Extensions;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 Created by andrey on 19.09.17.
 */
public interface ScriptEngineUtil {

	static ScriptEngine resolve(final Path scenarioPath) {

		ScriptEngine scriptEngine = null;

		// init the available external script engines
		final ScriptEngineManager scriptEngineManager = new ScriptEngineManager(
			Extensions.CLS_LOADER
		);

		// 1st try to determine the scenario type by the scenario file extension
		String scenarioFileExt = scenarioPath.toString();
		int dotPos = scenarioFileExt.lastIndexOf('.');
		if(dotPos > 0) {
			scenarioFileExt = scenarioFileExt.substring(dotPos + 1);
			scriptEngine = scriptEngineManager.getEngineByExtension(scenarioFileExt);
		}

		if(scriptEngine == null) {
			// 2nd: try to determine the scenario MIME type
			try {
				final String scenarioMimeType = Files.probeContentType(scenarioPath);
				if(scenarioMimeType != null) {
					scriptEngine = scriptEngineManager.getEngineByMimeType(scenarioMimeType);
				}
			} catch(final IOException e) {
				LogUtil.exception(
					Level.WARN, e,
					"Failed to determine the content type for the scenario file \"{}\"",
					scenarioPath
				);
			}
		}

		if(scriptEngine == null) {
			// 3rd: consider the scenario file a Javascript file
			Loggers.MSG.debug(
				"Unable to determine the scenario type for the file \"{}\"", scenarioPath
			);
			scriptEngine = scriptEngineManager.getEngineByName("js");
		}

		return scriptEngine;
	}

}
