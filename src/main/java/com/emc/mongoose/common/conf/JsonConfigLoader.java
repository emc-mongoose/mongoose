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
	private final static Logger LOG = LogManager.getLogger();
	private final static Set<String> mongooseKeys = new HashSet<>();
	//
	public enum JsonConfigLoaderActions {
		LOAD, UPDATE, UPLOAD
	}
	//
	private static RunTimeConfig DEFAULT_CFG;
	private static JsonConfigLoaderActions ACTION = JsonConfigLoaderActions.LOAD;
	//
	private final RunTimeConfig tgtConfig;
	//
	public JsonConfigLoader(final RunTimeConfig tgtConfig) {
		this.tgtConfig = tgtConfig;
	}
	//
	public static void loadPropsFromJsonCfgFile(final Path propsDir, final RunTimeConfig tgtConfig) {
		DEFAULT_CFG = tgtConfig;
		//
		final File cfgFile = propsDir.toFile();
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
			walkJsonTree(rootNode);
			tgtConfig.setMongooseKeys(mongooseKeys);
			tgtConfig.setJsonNode(rootNode);
			//
			if (ACTION.equals(JsonConfigLoaderActions.UPLOAD)) {
				LOG.debug("Going to update the configuration file \"{}\"", cfgFile.toString());
				jsonMapper.writerWithDefaultPrettyPrinter().writeValue(cfgFile, rootNode);
			}
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to load properties from \"{}\"", cfgFile
			);
		}
	}
	//
	private static void walkJsonTree(final JsonNode jsonNode) {
		walkJsonTree(jsonNode, null);
	}
	//
	private static void walkJsonTree(final JsonNode jsonNode, final String fullFileName) {
		final Iterator<String> fields = jsonNode.fieldNames();
		while (fields.hasNext()) {
			final String shortFieldName = fields.next();
			String fullFieldName = "";
			if (fullFileName != null) {
				if (fullFileName.isEmpty()) {
					fullFieldName = shortFieldName;
				} else {
					fullFieldName = fullFileName + Constants.DOT + shortFieldName;
				}
			}
			if (!jsonNode.get(shortFieldName).fieldNames().hasNext()) {
				if (ACTION.equals(JsonConfigLoaderActions.UPDATE)
						|| ACTION.equals(JsonConfigLoaderActions.UPLOAD)) {
					final Object value = DEFAULT_CFG.getProperty(fullFieldName);
					//
					if (!fullFieldName.startsWith(RunTimeConfig.PREFIX_KEY_ALIASING)) {
						LOG.trace(Markers.MSG, "Update property: \"{}\" = {}",
							fullFieldName, value);
					}
					DEFAULT_CFG.setProperty(fullFieldName, value);
					putJsonFormatValue(jsonNode, shortFieldName, fullFieldName);
				} else {
					if (!fullFieldName.startsWith(RunTimeConfig.PREFIX_KEY_ALIASING)) {
					LOG.trace(Markers.MSG, "Read property: \"{}\" = {}",
						fullFieldName, jsonNode.get(shortFieldName).toString());
					}
					if (!jsonNode.get(shortFieldName).isNull()) {
						DEFAULT_CFG.setProperty(fullFieldName, jsonNode.get(shortFieldName).toString()
							.replace("[", "")
							.replace("]", "")
							.replace(" ", "")
							.replace("\"", "")
							.trim());
					} else {
						DEFAULT_CFG.setProperty(fullFieldName, null);
					}
					mongooseKeys.add(fullFieldName);
				}
			} else {
				walkJsonTree(jsonNode.get(shortFieldName), fullFieldName);
			}
		}
	}
	//
	private static void putJsonFormatValue(
		final JsonNode node, final String shortFieldName, final String fullFieldName
	) {
		final JsonNode property = node.get(shortFieldName);
		final ObjectNode objectNode = (ObjectNode) node;
		//
		if (property.isTextual()) {
			objectNode.put(shortFieldName, DEFAULT_CFG.getString(fullFieldName));
		} else if (property.isNumber()) {
			objectNode.put(shortFieldName, DEFAULT_CFG.getInt(fullFieldName));
		} else if (property.isArray()) {
			final ArrayNode arrayNode = objectNode.putArray(shortFieldName);
			final String values[] = DEFAULT_CFG.getStringArray(fullFieldName);
			for (final String value : values) {
				arrayNode.add(value);
			}
		} else if (property.isNull()) {
			final Object value = DEFAULT_CFG.getProperty(fullFieldName);
			if (value != null) {
				objectNode.put(shortFieldName, value.toString());
			}
		}
	}
	//
	public static void updateProps(final RunTimeConfig tgtConfig, final boolean isUpload) {
		if (isUpload) {
			ACTION = JsonConfigLoaderActions.UPLOAD;
		} else {
			ACTION = JsonConfigLoaderActions.UPDATE;
		}
		DEFAULT_CFG = tgtConfig;
		walkJsonTree(tgtConfig.getJsonNode(), null);
	}
}
