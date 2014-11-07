package com.emc.mongoose.run;
//
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import com.emc.mongoose.util.logging.MessageFactoryImpl;
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
	private final Logger log;
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
	private final RunTimeConfig runTimeConfig;
	//
	public Scenario() {
		this.runTimeConfig = Main.RUN_TIME_CONFIG;
		//
		log = LogManager.getLogger(new MessageFactoryImpl(runTimeConfig));
	}
	//
	public Scenario(final RunTimeConfig runTimeConfig) {
		this.runTimeConfig = runTimeConfig;
		//
		log = LogManager.getLogger(new MessageFactoryImpl(runTimeConfig));
	}
	//
	public void run() {
		// get scenario language
		String scriptLangKey = null;
		try {
			scriptLangKey = runTimeConfig.getRunScenarioLang();
		} catch(final NoSuchElementException e) {
			log.fatal(
					Markers.ERR,
					"Scenario language not specified, use \"-Drun.scenario.lang=(js|py)\" argument"
			);
			System.exit(1);
		}
		// get scenario name
		String scriptName = null;
		try {
			scriptName = runTimeConfig.getRunScenarioName();
			log.info(Markers.MSG, "Script name to run: \"{}\"", scriptName);
		} catch(final NoSuchElementException e) {
			log.fatal(
					Markers.ERR,
					"Scenario language not specified, use \"-Drun.scenario.name=<NAME>\" argument"
			);
			System.exit(1);
		}
		//
		String scriptsRootDir = null;
		if(scriptName!=null && scriptLangKey!=null) {
			try {
				scriptsRootDir = runTimeConfig.getRunScenarioDir();
			} catch(final NoSuchElementException e) {
				log.fatal(Markers.ERR, "Scenario directory not specified");
				System.exit(1);
			}
			//
			final Path scriptDir = Paths.get(Main.DIR_ROOT, scriptsRootDir, scriptLangKey);
			if(VALUE_PY.equals(scriptLangKey)) {
				System.setProperty(KEY_PYTHON_PATH, scriptDir.toString());
				log.debug(
						Markers.MSG, "Set \"{}\"=\"{}\"",
						KEY_PYTHON_PATH, System.getProperty(KEY_PYTHON_PATH)
				);
			}
			//
			final Path scriptPath = Paths.get(scriptDir.toString(), scriptName+'.'+scriptLangKey);
			log.debug(Markers.MSG, "Using scenario from file {}", scriptPath);
			//
			if(Files.exists(scriptPath)) {
				log.debug(Markers.MSG, "File \"{}\" exists", scriptPath);
			} else {
				log.fatal(Markers.ERR, "File \"{}\" doesn't exist", scriptPath);
			}
			//
			if(Files.isReadable(scriptPath)) {
				log.debug(Markers.MSG, "File \"{}\" is readable", scriptPath);
			} else {
				log.fatal(Markers.ERR, "File \"{}\" is not readable", scriptPath);
			}
			//
			final String scriptLangValue = SCRIPT_LANG_MAP.get(scriptLangKey);
			if(scriptLangValue==null) {
				log.fatal(
						Markers.MSG, "Failed to determine the scenario language for key \"{}\"",
						scriptLangKey
				);
			} else {
				ScriptEngine scriptEngine = SCRIPT_ENGINE_MANAGER
					.getEngineByName(scriptLangValue);
				//
				if(scriptEngine==null) {
					log.fatal(
							Markers.ERR,
							"Failed to get script engine for language \"{}\", the available engines are:",
							scriptLangValue
					);
					for(final ScriptEngineFactory sef : SCRIPT_ENGINE_MANAGER.getEngineFactories()) {
						log.info(
								Markers.ERR, "\t{}:\tfor language \"{}\" v{}",
								sef.getEngineName(), sef.getLanguageName(), sef.getLanguageVersion()
						);
					}
				} else {
					try {
						log.debug(Markers.MSG, "Script start");
						scriptEngine.eval(Files.newBufferedReader(scriptPath, Charset.defaultCharset()));
						log.debug(Markers.MSG, "Script from \"{}\" done", scriptPath);
					} catch(final ScriptException e) {
						ExceptionHandler.trace(log, Level.WARN, e, "Script failure");
					} catch(final FileNotFoundException e) {
						log.error(Markers.ERR, "Script file not found at \"{}\"", scriptPath);
					} catch(final IOException e) {
						log.error(Markers.ERR, "Script file reading failure", e);
					}
				}
				//
			}
		}
	}
}
