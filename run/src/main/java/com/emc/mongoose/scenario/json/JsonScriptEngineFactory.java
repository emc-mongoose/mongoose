package com.emc.mongoose.scenario.json;

import com.emc.mongoose.ui.config.Config;

import javax.script.ScriptEngineFactory;
import java.util.ArrayList;
import java.util.List;

/**
 Created by andrey on 18.09.17.
 */
public class JsonScriptEngineFactory
implements ScriptEngineFactory {

	public static final String ENGINE_NAME = "mongoose";
	public static final String ENGINE_VERSION = "3.5.0";
	public static final ArrayList<String> EXTENSIONS = new ArrayList<>();
	public static final List<String> MIME_TYPES = new ArrayList<>();
	public static final List<String> NAMES = new ArrayList<>();
	static {
		EXTENSIONS.add("json");
		MIME_TYPES.add("application/json");
		MIME_TYPES.add("text/x-json");
		NAMES.add(ENGINE_NAME);
		NAMES.add("json");
	}

	private final Config config;

	public JsonScriptEngineFactory(final Config config) {
		this.config = config;
	}

	@Override
	public String getEngineName() {
		return ENGINE_NAME;
	}

	@Override
	public String getEngineVersion() {
		return ENGINE_VERSION;
	}

	@Override
	public List<String> getExtensions() {
		return EXTENSIONS;
	}

	@Override
	public List<String> getMimeTypes() {
		return MIME_TYPES;
	}

	@Override
	public List<String> getNames() {
		return NAMES;
	}

	@Override
	public String getLanguageName() {
		return ENGINE_NAME;
	}

	@Override
	public String getLanguageVersion() {
		return ENGINE_VERSION;
	}

	@Override
	public Object getParameter(final String key) {
		return null;
	}

	@Override
	public String getMethodCallSyntax(final String obj, final String m, final String... args) {
		return null;
	}

	@Override
	public String getOutputStatement(final String toDisplay) {
		return null;
	}

	@Override
	public String getProgram(final String... statements) {
		return null;
	}

	@Override
	public JsonScriptEngine getScriptEngine() {
		return new JsonScriptEngine(config);
	}
}
