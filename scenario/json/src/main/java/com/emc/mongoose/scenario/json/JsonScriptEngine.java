package com.emc.mongoose.scenario.json;

import com.emc.mongoose.scenario.ScenarioParseException;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.scenario.Constants.ATTR_CONFIG;

import org.apache.logging.log4j.Level;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 Created by andrey on 18.09.17.
 */
public class JsonScriptEngine
extends AbstractScriptEngine
implements ScriptEngine {

	private final JsonScriptEngineFactory factory;

	public JsonScriptEngine(final JsonScriptEngineFactory factory) {
		this.factory = factory;
	}

	@Override
	public Object eval(final String script, final ScriptContext context)
	throws ScriptException {
		final Config config = (Config) context.getAttribute(ATTR_CONFIG);
		try(final Scenario jsonScenario = new JsonScenario(config, script)) {
			jsonScenario.run();
		} catch(final IOException | ScenarioParseException e) {
			throw new ScriptException(e);
		}
		return null;
	}

	@Override
	public Object eval(final Reader reader, final ScriptContext context)
	throws ScriptException {
		final StringBuilder scriptLines = new StringBuilder();
		try(final BufferedReader br = new BufferedReader(reader)) {
			String nextLine;
			while(null != (nextLine = br.readLine())) {
				scriptLines.append(nextLine);
			}
			return eval(scriptLines.toString(), context);
		} catch(final IOException e) {
			LogUtil.exception(Level.ERROR, e, "Failed to read the scenario content");
		}
		return null;
	}

	@Override
	public Bindings createBindings() {
		return new SimpleBindings();
	}

	@Override
	public JsonScriptEngineFactory getFactory() {
		return factory;
	}
}
