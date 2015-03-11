package com.emc.mongoose.run;
//
import com.emc.mongoose.core.impl.util.RunTimeConfig;
import com.emc.mongoose.core.impl.persist.TraceLogger;
import com.emc.mongoose.core.api.persist.Markers;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
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
		KEY_PYTHON_PATH = "python.path",
		VALUE_JS = "js",
		VALUE_PY = "py";
	//
	private final static ScriptEngineManager SCRIPT_ENGINE_MANAGER = new ScriptEngineManager();
	private final static HashMap<String, String> SCRIPT_LANG_MAP = new HashMap<>();
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
			scriptLangKey = localRunTimeConfig.getRunScenarioLang();
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
			scriptName = localRunTimeConfig.getRunScenarioName();
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
		if(scriptName!=null && scriptLangKey!=null) {
			try {
				scriptsRootDir = localRunTimeConfig.getRunScenarioDir();
			} catch(final NoSuchElementException e) {
				LOG.fatal(Markers.ERR, "Scenario directory not specified");
				System.exit(1);
			}
			//
			final Path scriptDir = Paths.get(Main.DIR_ROOT, scriptsRootDir, scriptLangKey);
			if(VALUE_PY.equals(scriptLangKey)) {
				System.setProperty(KEY_PYTHON_PATH, scriptDir.toString());
				LOG.debug(
					Markers.MSG, "Set \"{}\"=\"{}\"",
					KEY_PYTHON_PATH, System.getProperty(KEY_PYTHON_PATH)
				);
			}
			//
			final Path scriptPath = Paths.get(scriptDir.toString(), scriptName+'.'+scriptLangKey);
			LOG.debug(Markers.MSG, "Using scenario from file {}", scriptPath);
			//
			if(Files.exists(scriptPath)) {
				LOG.debug(Markers.MSG, "File \"{}\" exists", scriptPath);
			} else {
				LOG.fatal(Markers.ERR, "File \"{}\" doesn't exist", scriptPath);
			}
			//
			if(Files.isReadable(scriptPath)) {
				LOG.debug(Markers.MSG, "File \"{}\" is readable", scriptPath);
			} else {
				LOG.fatal(Markers.ERR, "File \"{}\" is not readable", scriptPath);
			}
			//
			final String scriptLangValue = SCRIPT_LANG_MAP.get(scriptLangKey);
			if(scriptLangValue==null) {
				LOG.fatal(
					Markers.MSG, "Failed to determine the scenario language for key \"{}\"",
					scriptLangKey
				);
			} else {
				ScriptEngine scriptEngine = SCRIPT_ENGINE_MANAGER
					.getEngineByName(scriptLangValue);
				//
				if(scriptEngine == null) {

					for(final ScriptEngineFactory sef : SCRIPT_ENGINE_MANAGER.getEngineFactories()) {
						LOG.info(
							Markers.ERR, "\t{}:\tfor language \"{}\" v{}",
							sef.getEngineName(), sef.getLanguageName(), sef.getLanguageVersion()
						);
						if(scriptLangValue.equals(sef.getEngineName())) {
							scriptEngine = sef.getScriptEngine();
							break;
						}
					}
					if(scriptEngine == null) {
						LOG.fatal(
							Markers.ERR,
							"Failed to get script engine for language \"{}\", the available engines are:",
							scriptLangValue
						);
					}
				} else {
					try {
						LOG.debug(Markers.MSG, "Script start");
						scriptEngine.eval(Files.newBufferedReader(scriptPath, Charset.defaultCharset()));
						LOG.debug(Markers.MSG, "Script from \"{}\" done", scriptPath);
					} catch(final ScriptException e) {
						TraceLogger.failure(LOG, Level.WARN, e, "Script failure");
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
