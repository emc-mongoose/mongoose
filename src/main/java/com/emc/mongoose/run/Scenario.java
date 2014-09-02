package com.emc.mongoose.run;
//
import com.emc.mongoose.conf.RunTimeConfig;
import com.emc.mongoose.logging.Markers;
//
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
 */
public final class Scenario {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static String
		KEY_PYTHON_PATH = "python.path",
		VALUE_PY = "py";
	//
	private final static ScriptEngineManager SCRIPT_ENGINE_MANAGER = new ScriptEngineManager();
	static {
		LOG.debug(Markers.MSG, "Available script engines follow below:");
		for(final ScriptEngineFactory sef: SCRIPT_ENGINE_MANAGER.getEngineFactories()) {
			LOG.debug(
				Markers.MSG, "\t* {} v{} by {}",
				sef.getLanguageName(), sef.getLanguageVersion(), sef.getEngineName()
			);
		}
	}
	private final static HashMap<String, String> SCRIPT_LANG_MAP = new HashMap<>();
	static {
		SCRIPT_LANG_MAP.put("js", "ECMAScript");
		SCRIPT_LANG_MAP.put(VALUE_PY, "python");
	}
	//
	public static void run() {
		// get scenario language
		String scriptLang = null;
		try {
			scriptLang = RunTimeConfig.getString("run.scenario.lang");
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
			scriptName = RunTimeConfig.getString("run.scenario.name");
			LOG.info(Markers.MSG, "Script name to run: \"{}\"", scriptName);
		} catch(final NoSuchElementException e) {
			LOG.fatal(
				Markers.ERR,
				"Scenario language not specified, use \"-Drun.scenario.name=<NAME>\" argument"
			);
			System.exit(1);
		}
		//
		String scriptsRootDir = null;
		if(scriptName!=null && scriptLang!=null) {
			try {
				scriptsRootDir = RunTimeConfig.getString("run.scenario.dir");
			} catch(final NoSuchElementException e) {
				LOG.fatal(Markers.ERR, "Scenario directory not specified");
				System.exit(1);
			}
			//
			final Path scriptDir = Paths.get(Main.DIR_ROOT, scriptsRootDir, scriptLang);
			if(VALUE_PY.equals(scriptLang)) {
				System.setProperty(KEY_PYTHON_PATH, scriptDir.toString());
				LOG.debug(
					Markers.MSG, "Set \"{}\"=\"{}\"",
					KEY_PYTHON_PATH, System.getProperty(KEY_PYTHON_PATH)
				);
			}
			//
			final Path scriptPath = Paths.get(scriptDir.toString(), scriptName+'.'+scriptLang);
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
			final ScriptEngine scriptEngine = SCRIPT_ENGINE_MANAGER.getEngineByName(
				SCRIPT_LANG_MAP.get(scriptLang)
			);
			//
			if(scriptEngine==null) {
				LOG.warn(Markers.ERR, "Failed to get script engine for \"{}\"", scriptPath);
			} else {
				try {
					LOG.debug(Markers.MSG, "Script start");
					scriptEngine.eval(Files.newBufferedReader(scriptPath, Charset.defaultCharset()));
					LOG.debug(Markers.MSG, "Script from \"{}\" done", scriptPath);
				} catch(final ScriptException e) {
					LOG.error(Markers.ERR, "Script failure: {}", e.toString());
					if(LOG.isDebugEnabled()) {
						final Throwable cause = e.getCause();
						if(cause!=null) {
							LOG.debug(Markers.ERR, cause.toString(), cause.getCause());
						}
					}
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
