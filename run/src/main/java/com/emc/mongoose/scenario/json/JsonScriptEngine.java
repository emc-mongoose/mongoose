package com.emc.mongoose.scenario.json;

import com.emc.mongoose.scenario.Scenario;
import com.emc.mongoose.scenario.ScenarioParseException;
import com.emc.mongoose.ui.config.Config;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 Created by andrey on 18.09.17.
 */
public class JsonScriptEngine
implements ScriptEngine {

	private final Map<String, Object> m = new HashMap<>();
	private final Config config;

	public JsonScriptEngine(final Config config) {
		this.config = config;
	}

	@Override
	public Object eval(final String script, final ScriptContext context)
	throws ScriptException {
		throw new AssertionError("Not implemented");
	}

	@Override
	public Object eval(final Reader reader, final ScriptContext context)
	throws ScriptException {
		throw new AssertionError("Not implemented");
	}

	@Override
	public Object eval(final String script)
	throws ScriptException {
		try(final Scenario jsonScenario = new JsonScenario(config, script)) {
			jsonScenario.run();
		} catch(final IOException | ScenarioParseException e) {
			throw new ScriptException(e);
		}
		return null;
	}

	@Override
	public Object eval(final Reader reader)
	throws ScriptException {
		throw new AssertionError("Not implemented");
	}

	@Override
	public Object eval(final String script, final Bindings n)
	throws ScriptException {
		throw new AssertionError("Not implemented");
	}

	@Override
	public Object eval(final Reader reader, final Bindings n)
	throws ScriptException {
		throw new AssertionError("Not implemented");
	}

	@Override
	public void put(final String key, final Object value) {
		m.put(key, value);
	}

	@Override
	public Object get(final String key) {
		return m.get(key);
	}

	@Override
	public Bindings getBindings(final int scope) {
		return null;
	}

	@Override
	public void setBindings(final Bindings bindings, final int scope) {
	}

	@Override
	public Bindings createBindings() {
		return new SimpleBindings();
	}

	@Override
	public ScriptContext getContext() {
		return null;
	}

	@Override
	public void setContext(final ScriptContext context) {
	}

	@Override
	public JsonScriptEngineFactory getFactory() {
		return new JsonScriptEngineFactory(config);
	}
}
