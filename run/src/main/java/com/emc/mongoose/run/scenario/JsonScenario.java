package com.emc.mongoose.run.scenario;

import com.emc.mongoose.run.scenario.step.Step;
import com.emc.mongoose.run.scenario.step.SequentialStep;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 Created by kurila on 02.02.16.
 */
public class JsonScenario
extends SequentialStep
implements Scenario {
	//
	public JsonScenario(final Config config, final File scenarioSrcFile)
	throws IOException, ScenarioParseException {
		this(
			config,
			new ObjectMapper()
				.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
				.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true)
				.<Map<String, Object>>readValue(
					scenarioSrcFile, new TypeReference<Map<String, Object>>(){}
				)
		);
	}
	//
	public JsonScenario(final Config config, final InputStream scenarioInputStream)
	throws IOException, ScenarioParseException {
		this(
			config,
			new ObjectMapper()
				.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
				.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true)
				.<Map<String, Object>>readValue(
					scenarioInputStream, new TypeReference<Map<String, Object>>(){}
				)
		);
	}
	//
	public JsonScenario(final Config config, final String scenarioString)
	throws IOException, ScenarioParseException {
		this(
			config,
			new ObjectMapper()
				.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
				.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true)
				.<Map<String, Object>>readValue(
					scenarioString, new TypeReference<Map<String, Object>>(){}
				)
		);
	}
	//
	public JsonScenario(final Config config, final Map<String, Object> tree)
	throws IOException, ScenarioParseException {
		super(config, overrideFromEnv(validateAgainstSchema(tree)));
	}
	//
	private static final Map<String, Object> validateAgainstSchema(final Map<String, Object> tree) {
		/*final Path schemaPath = Paths.get(
			BasicConfig.getWorkingDir(), DIR_SCENARIO, FNAME_SCENARIO_SCHEMA
		);
		try {
			final JsonSchema scenarioSchema = JsonSchemaFactory
				.newBuilder().freeze().getJsonSchema(schemaPath.toUri().toString());
			final JsonNode jacksonTree = new ObjectMapper().valueToTree(tree);
			scenarioSchema.validate(jacksonTree, true);
		} catch(final ProcessingException e) {
			LogUtil.exception(Level.WARN, e, "Failed to load the scenario schema");
		}*/
		return tree;
	}
	//
	private static final Pattern PATTERN_ENV_VAR = Pattern.compile(
		".*\\$\\{([\\w\\-_\\.!@#%\\^&\\*=\\+\\(\\)\\[\\]~:;'\\\\\\|/<>,\\?]+)\\}.*"
	);
	private static Map<String, Object> overrideFromEnv(final Map<String, Object> tree) {

		Object value;
		String valueStr;
		Matcher m;
		String propertyName;
		String newValue;
		boolean alteredFlag;

		for(final String key : tree.keySet()) {
			value = tree.get(key);
			if(value instanceof Map) {
				overrideFromEnv((Map<String, Object>) value);
			} else if(value instanceof List) {
				overrideFromEnv((List<Object>) value);
			} else if(value instanceof String) {
				valueStr = (String) value;
				m = PATTERN_ENV_VAR.matcher(valueStr);
				alteredFlag = false;
				while(m.find()) {
					propertyName = m.group(1);
					if(propertyName != null && !propertyName.isEmpty()) {
						newValue = System.getenv(propertyName);
						if(newValue != null) {
							valueStr = valueStr.replace("${" + propertyName + "}", newValue);
							alteredFlag = true;
							Loggers.MSG.info(
								"Key \"{}\": replaced \"{}\" with new value \"{}\"",
								key, propertyName, newValue
							);
						}
					}
				}
				if(alteredFlag) {
					tree.put(key, valueStr);
				}
			}
		}

		return tree;
	}
	//
	private static List<Object> overrideFromEnv(final List<Object> values) {

		Object value;
		String valueStr;
		Matcher m;
		String propertyName;
		String newValue;
		boolean alteredFlag;

		for(int i = 0; i < values.size(); i ++) {
			value = values.get(i);
			if(value instanceof Map) {
				overrideFromEnv((Map<String, Object>) value);
			} else if(value instanceof List) {
				overrideFromEnv((List) value);
			} else if(value instanceof String) {
				valueStr = (String) value;
				m = PATTERN_ENV_VAR.matcher(valueStr);
				alteredFlag = false;
				while(m.find()) {
					propertyName = m.group(1);
					if(propertyName != null && !propertyName.isEmpty()) {
						newValue = System.getenv(propertyName);
						if(newValue != null) {
							valueStr = valueStr.replace("${" + propertyName + "}", newValue);
							alteredFlag = true;
							Loggers.MSG.info(
								"Value #{}: replaced \"{}\" with new value \"{}\"",
								i, propertyName, newValue
							);
						}
					}
				}
				if(alteredFlag) {
					values.set(i, valueStr);
				}
			}
		}
		return values;
	}
	//
	@Override
	protected final void loadSubTree(final Map<String, Object> subTree)
	throws ScenarioParseException {
		appendNewJob(subTree, localConfig);
	}
	//
	@Override
	protected final synchronized boolean append(final Step step) {
		if(childSteps.size() == 0) {
			return super.append(step);
		} else {
			return false;
		}
	}
	//
	@Override
	protected final void invoke() {
		Loggers.MSG.info("Scenario start");
		super.invoke();
		Loggers.MSG.info("Scenario end");
	}
	//
	@Override
	public final String toString() {
		return "jsonScenario#" + hashCode();
	}
}
