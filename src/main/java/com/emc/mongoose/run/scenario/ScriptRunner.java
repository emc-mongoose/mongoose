package com.emc.mongoose.run.scenario;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
//
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
/**
 Created by kurila on 12.05.14.
 A scenario runner utility class.
 */
public final class ScriptRunner
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static String
		VALUE_JS = "js",
		VALUE_PY = "py",
		//
		KEY_PYTHON_PATH = "python.path",
		KEY_PYTHON_IMPORT_SITE = "python.import.site";
	//
	private final static ScriptEngineManager SCRIPT_ENGINE_MANAGER = new ScriptEngineManager();
	private final static Map<String, String> SCRIPT_LANG_MAP = new HashMap<>();
	static {
		SCRIPT_LANG_MAP.put(VALUE_JS, "ECMAScript");
		SCRIPT_LANG_MAP.put(VALUE_PY, "jython");
	}
	//
	public void run() {
		final RunTimeConfig localRunTimeConfig = RunTimeConfig.getContext();
		// get scenario language
		String scriptLangKey = null;
		try {
			scriptLangKey = localRunTimeConfig.getScenarioLang();
		} catch(final NoSuchElementException e) {
			LOG.fatal(
				Markers.ERR,
				"Scenario language not specified, use \"-Drun.scenario.lang=(js|py)\" argument"
			);
			System.exit(1);
		}
		// get scenario name
		String scriptName = null;
		try {
			scriptName = localRunTimeConfig.getScenarioName();
			//LOG.info(Markers.MSG, "Script name to run: \"{}\"", scriptName);
		} catch(final NoSuchElementException e) {
			LOG.fatal(
				Markers.ERR,
				"Scenario language not specified, use \"-Drun.scenario.name=<NAME>\" argument"
			);
			System.exit(1);
		}
		//
		String scriptsRootDir = null;
		if(scriptName != null && scriptLangKey != null) {
			try {
				scriptsRootDir = localRunTimeConfig.getScenarioDir();
			} catch(final NoSuchElementException e) {
				LOG.fatal(Markers.ERR, "Scenario directory not specified");
				System.exit(1);
			}
			//
			Path scriptDir = Paths.get(RunTimeConfig.DIR_ROOT, scriptsRootDir, scriptLangKey);
			if (!Files.exists(scriptDir)){
				LOG.info(Markers.MSG, "Directory \"{}\" doesn't exist. Try look for bundle directory", scriptDir);
				final ClassLoader classloader = ScriptRunner.class.getClassLoader();
				final URL bundleScriptDirURL = classloader.getResource("");
				if (bundleScriptDirURL != null) {
					scriptDir = Paths.get(bundleScriptDirURL.getPath(), scriptsRootDir, scriptLangKey);
				}
			}
			// language-specifig preparations
			switch(scriptLangKey) {
				case VALUE_JS:
					break;
				case VALUE_PY:
					System.setProperty(KEY_PYTHON_PATH, scriptDir.toString());
					LOG.debug(
						Markers.MSG, "Set system property \"{}\"=\"{}\"",
						KEY_PYTHON_PATH, System.getProperty(KEY_PYTHON_PATH)
					);
					System.setProperty(KEY_PYTHON_IMPORT_SITE, Boolean.toString(false));
					LOG.debug(
						Markers.MSG, "Set system property \"{}\"=\"{}\"",
						KEY_PYTHON_IMPORT_SITE, System.getProperty(KEY_PYTHON_IMPORT_SITE)
					);
					break;
				default:
					break;
			}
			//
			final Path scriptPath = Paths.get(scriptDir.toString(), scriptName+'.'+scriptLangKey);
			LOG.debug(Markers.MSG, "Using scenario from file {}", scriptPath);
			//
			if(Files.exists(scriptPath)) {
				LOG.debug(Markers.MSG, "File \"{}\" exists", scriptPath);
			} else {
				LOG.info(Markers.MSG, "File \"{}\" doesn't exist", scriptPath);
			}
			//
			if(Files.isReadable(scriptPath)) {
				LOG.debug(Markers.MSG, "File \"{}\" is readable", scriptPath);
			} else {
				LOG.fatal(Markers.ERR, "File \"{}\" is not readable", scriptPath);
			}
			//
			final String scriptLangValue = SCRIPT_LANG_MAP.get(scriptLangKey);
			if(scriptLangValue == null) {
				LOG.fatal(
					Markers.MSG, "Failed to determine the scenario language for key \"{}\"",
					scriptLangKey
				);
			} else {
				ScriptEngine scriptEngine = SCRIPT_ENGINE_MANAGER.getEngineByName(scriptLangValue);
				//
				if(scriptEngine == null) {
					for(final ScriptEngineFactory sef : SCRIPT_ENGINE_MANAGER.getEngineFactories()) {
						LOG.info(
							Markers.ERR, "\t{}:\tfor language \"{}\" v{}",
							sef.getEngineName(), sef.getLanguageName(), sef.getLanguageVersion()
						);
						if(scriptLangValue.equals(sef.getEngineName())) {
							scriptEngine = sef.getScriptEngine();
							LOG.info(
								Markers.MSG, "Required script engine found: \"{}\"", scriptEngine
							);
							break;
						}
					}
					if(scriptEngine == null) {
						LOG.fatal(
							Markers.ERR, "Failed to get script engine for language \"{}\"",
							scriptLangValue
						);
					}
				} else {
					try {
						LOG.debug(Markers.MSG, "Script start");
						scriptEngine.eval(Files.newBufferedReader(scriptPath, StandardCharsets.UTF_8));
						LOG.debug(Markers.MSG, "Script from \"{}\" done", scriptPath);
					} catch(final ScriptException e) {
						LogUtil.exception(LOG, Level.WARN, e, "Script failure");
						e.printStackTrace(System.err);
					} catch(final FileNotFoundException e) {
						LOG.error(Markers.ERR, "Script file not found at \"{}\"", scriptPath);
					} catch(final IOException e) {
						LOG.error(Markers.ERR, "Script file reading failure", e);
					}
				}
				//
			}
		}
	}
}
