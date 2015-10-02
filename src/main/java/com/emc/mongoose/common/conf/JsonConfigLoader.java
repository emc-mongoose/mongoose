package com.emc.mongoose.common.conf;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
//
import org.apache.commons.configuration.ConversionException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
//
/**
 * Created by gusakk on 3/13/15.
 */
public class JsonConfigLoader {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	private final RunTimeConfig rtConfig;
	private final Set<String> mongooseKeys;
	//
	public enum JsonConfigLoaderActions {
		// load configuration from file
		LOAD,
		// update jsonNode w/ current configuration parameters
		UPDATE
	}
	private JsonConfigLoaderActions action = JsonConfigLoaderActions.LOAD;
	//
	public JsonConfigLoader(final RunTimeConfig rtConfig) {
		this.rtConfig = rtConfig;
		final Set<String> keys = rtConfig.getMongooseKeys();
		mongooseKeys = keys.isEmpty() ? keys : new HashSet<String>();
	}
	//
	public void loadPropsFromJsonCfgFile(final Path filePath) {
		final File cfgFile = filePath.toFile();
		final ObjectMapper jsonMapper = new ObjectMapper();
		//
		try {
			JsonNode rootNode;
			if(cfgFile.exists() && cfgFile.isFile()){
				LOG.debug(Markers.MSG, "Load the config from json file \"{}\"", cfgFile.toString());
				rootNode = jsonMapper.readTree(cfgFile);
			} else {
				final ClassLoader cl = JsonConfigLoader.class.getClassLoader();
				final InputStream bundledConf = cl.getResourceAsStream(RunTimeConfig.FNAME_CONF);
				LOG.debug(
					Markers.MSG, "Load the bundled config", cl.getResource(RunTimeConfig.FNAME_CONF)
				);
				rootNode = jsonMapper.readTree(bundledConf);
			}
			//
			action = JsonConfigLoaderActions.LOAD;
			walkJsonTree(rootNode);
			rtConfig.setMongooseKeys(mongooseKeys);
			rtConfig.setJsonNode(rootNode);
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to load properties from \"{}\"", cfgFile
			);
		}
	}
	//
	private void walkJsonTree(final JsonNode jsonNode) {
		walkJsonTree(jsonNode, null);
	}
	//
	private void walkJsonTree(final JsonNode jsonNode, final String fieldPrefix) {
		final Iterator<String> fields = jsonNode.fieldNames();
		while(fields.hasNext()) {
			final String jsonField = fields.next();
			String propertyName = "";
			if(fieldPrefix != null) {
				if(fieldPrefix.isEmpty()) {
					propertyName = jsonField;
				} else {
					propertyName = fieldPrefix + Constants.DOT + jsonField;
				}
			}
			if(!jsonNode.get(jsonField).fieldNames().hasNext()) {
				if(action.equals(JsonConfigLoaderActions.UPDATE)) {
					final Object value = rtConfig.getProperty(propertyName);
					if(!propertyName.startsWith(RunTimeConfig.PREFIX_KEY_ALIASING)) {
						LOG.trace(Markers.MSG, "Update property: \"{}\" = {}",
							propertyName, value);
					}
					rtConfig.setProperty(propertyName, value);
					putJsonFormatValue(jsonNode, jsonField, propertyName);
				} else {
					// load configuration from mongoose.json
					final JsonNode nodeValue = jsonNode.get(jsonField);
					if(!propertyName.startsWith(RunTimeConfig.PREFIX_KEY_ALIASING)) {
						LOG.trace(Markers.MSG, "Read property: \"{}\" = {}",
							propertyName, nodeValue);
					}
					//
					if(!nodeValue.isNull()) {
						rtConfig.setProperty(
							propertyName, getFormattedValue(nodeValue.toString())
						);
					} else {
						rtConfig.setProperty(propertyName, null);
					}
					mongooseKeys.add(propertyName);
				}
			} else {
				walkJsonTree(jsonNode.get(jsonField), propertyName);
			}
		}
	}
	//
	public JsonNode updateJsonNode() {
		action = JsonConfigLoaderActions.UPDATE;
		final JsonNode rootNode = rtConfig.getJsonNode();
		//
		if(rootNode == null) {
			throw new IllegalArgumentException(
				"Properties should be loaded from cfg file before updating"
			);
		}
		walkJsonTree(rootNode);
		return rootNode;
	}
	//
	public void updateJsonCfgFile(final File cfgFile) {
		final JsonNode rootNode = updateJsonNode();
		final ObjectMapper jsonMapper = new ObjectMapper();
		LOG.debug("Going to update the configuration file \"{}\"", cfgFile.toString());
		try {
			jsonMapper.writerWithDefaultPrettyPrinter().writeValue(cfgFile, rootNode);
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to update properties in \"{}\"", cfgFile);
		}
	}
	//
	private void putJsonFormatValue(
		final JsonNode node, final String jsonField, final String propertyName
	) {
		final JsonNode property = node.get(jsonField);
		final ObjectNode objectNode = (ObjectNode) node;
		//
		try {
			if(property.isTextual()) {
				final String stringValue = rtConfig.getProperty(propertyName).toString();
				objectNode.put(jsonField, getFormattedValue(stringValue));
			} else if(property.isNumber()) {
				objectNode.put(jsonField, rtConfig.getInt(propertyName));
			} else if(property.isArray()) {
				final ArrayNode arrayNode = objectNode.putArray(jsonField);
				final String values[] = rtConfig.getStringArray(propertyName);
				for(final String value : values) {
					arrayNode.add(value);
				}
			} else if(property.isBoolean()) {
				objectNode.put(jsonField, rtConfig.getBoolean(propertyName));
			} else if(property.isNull()) {
				final Object value = rtConfig.getProperty(propertyName);
				if(value != null) {
					objectNode.put(jsonField, value.toString());
				}
			}
		} catch(final ConversionException e) {
			final String stringValue = rtConfig.getProperty(propertyName).toString();
			objectNode.put(jsonField, getFormattedValue(stringValue));
		} catch(final NullPointerException e) {
			LogUtil.exception(LOG, Level.WARN, e,
				"rtConfig doesn't contain \"{}\" property", propertyName);
		}
	}
	//
	private static String getFormattedValue(final String value) {
		return value
			.replace("[", "")
			.replace("]", "")
			.replace(" ", "")
			.replace("\"", "")
			.trim();
	}
}
