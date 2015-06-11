package com.emc.mongoose.run.scenario;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.FileNotFoundException;
import java.io.IOException;
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
public final class Scenario
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
				LogUtil.ERR,
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
				LogUtil.ERR,
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
				LOG.fatal(LogUtil.ERR, "Scenario directory not specified");
				System.exit(1);
			}
			//
			final Path scriptDir = Paths.get(RunTimeConfig.DIR_ROOT, scriptsRootDir, scriptLangKey);
			// language-specifig preparations
			switch(scriptLangKey) {
				case VALUE_JS:
					break;
				case VALUE_PY:
					System.setProperty(KEY_PYTHON_PATH, scriptDir.toString());
					LOG.debug(
						LogUtil.MSG, "Set system property \"{}\"=\"{}\"",
						KEY_PYTHON_PATH, System.getProperty(KEY_PYTHON_PATH)
					);
					System.setProperty(KEY_PYTHON_IMPORT_SITE, Boolean.toString(false));
					LOG.debug(
						LogUtil.MSG, "Set system property \"{}\"=\"{}\"",
						KEY_PYTHON_IMPORT_SITE, System.getProperty(KEY_PYTHON_IMPORT_SITE)
					);
					break;
				default:
					break;
			}
			//
			final Path scriptPath = Paths.get(scriptDir.toString(), scriptName+'.'+scriptLangKey);
			LOG.debug(LogUtil.MSG, "Using scenario from file {}", scriptPath);
			//
			if(Files.exists(scriptPath)) {
				LOG.debug(LogUtil.MSG, "File \"{}\" exists", scriptPath);
			} else {
				LOG.fatal(LogUtil.ERR, "File \"{}\" doesn't exist", scriptPath);
			}
			//
			if(Files.isReadable(scriptPath)) {
				LOG.debug(LogUtil.MSG, "File \"{}\" is readable", scriptPath);
			} else {
				LOG.fatal(LogUtil.ERR, "File \"{}\" is not readable", scriptPath);
			}
			//
			final String scriptLangValue = SCRIPT_LANG_MAP.get(scriptLangKey);
			if(scriptLangValue == null) {
				LOG.fatal(
					LogUtil.MSG, "Failed to determine the scenario language for key \"{}\"",
					scriptLangKey
				);
			} else {
				ScriptEngine scriptEngine = SCRIPT_ENGINE_MANAGER.getEngineByName(scriptLangValue);
				//
				if(scriptEngine == null) {
					for(final ScriptEngineFactory sef : SCRIPT_ENGINE_MANAGER.getEngineFactories()) {
						LOG.info(
							LogUtil.ERR, "\t{}:\tfor language \"{}\" v{}",
							sef.getEngineName(), sef.getLanguageName(), sef.getLanguageVersion()
						);
						if(scriptLangValue.equals(sef.getEngineName())) {
							scriptEngine = sef.getScriptEngine();
							LOG.info(
								LogUtil.MSG, "Required script engine found: \"{}\"", scriptEngine
							);
							break;
						}
					}
					if(scriptEngine == null) {
						LOG.fatal(
							LogUtil.ERR, "Failed to get script engine for language \"{}\"",
							scriptLangValue
						);
					}
				} else {
					try {
						LOG.debug(LogUtil.MSG, "Script start");
						scriptEngine.eval(Files.newBufferedReader(scriptPath, StandardCharsets.UTF_8));
						LOG.debug(LogUtil.MSG, "Script from \"{}\" done", scriptPath);
					} catch(final ScriptException e) {
						LogUtil.exception(LOG, Level.WARN, e, "Script failure");
						e.printStackTrace(System.err);
					} catch(final FileNotFoundException e) {
						LOG.error(LogUtil.ERR, "Script file not found at \"{}\"", scriptPath);
					} catch(final IOException e) {
						LOG.error(LogUtil.ERR, "Script file reading failure", e);
					}
				}
				//
			}
		}
	}
}
