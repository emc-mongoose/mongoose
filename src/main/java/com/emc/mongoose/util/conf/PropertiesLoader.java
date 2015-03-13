package com.emc.mongoose.util.conf;

import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.logging.TraceLogger;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by gusakk on 3/13/15.
 */
public class PropertiesLoader {
	//
	private final static Logger LOG = LogManager.getLogger();
	private static RunTimeConfig DEFAULT_CFG;
	private static String ACTION = "load";
	//
	private final RunTimeConfig tgtConfig;
	//
	public PropertiesLoader(final RunTimeConfig tgtConfig) {
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
			LOG.debug(Markers.MSG, "Load system properties from json file \"{}\"", cfgFile.toString());
			final JsonNode rootNode = jsonMapper.readTree(cfgFile);
			walkJsonTree(rootNode);
			//
			//jsonMapper.writer(pp).writeValue(cfgFile, rootNode);
		} catch (IOException e) {
			TraceLogger.failure(LOG, Level.ERROR, e,
					String.format("Failed to load properties from \"%s\"", cfgFile.toString()));
		}
	}
	//
	private static void walkJsonTree(final JsonNode jsonNode) {
		walkJsonTree(jsonNode, null);
	}
	//
	private static void walkJsonTree(final JsonNode jsonNode, final String fullKeyName) {
		final Iterator<String> fields = jsonNode.fieldNames();
		while (fields.hasNext()) {
			final String shortFieldName = fields.next();
			String fullFieldName = "";
			if (fullKeyName != null) {
				if (fullKeyName.isEmpty()) {
					fullFieldName = shortFieldName;
				} else {
					fullFieldName = fullKeyName + Main.DOT + shortFieldName;
				}
			}
			if (!jsonNode.get(shortFieldName).fieldNames().hasNext()) {
				if (ACTION.equals("update")) {
					DEFAULT_CFG.setProperty(fullFieldName, DEFAULT_CFG.getProperty(fullFieldName));
				} else {
					DEFAULT_CFG.setProperty(fullFieldName, jsonNode.get(shortFieldName).toString()
							.replace("\"", "")
							.trim());

					((ObjectNode) jsonNode).put(shortFieldName, "a");
				}
			} else {
				walkJsonTree(jsonNode.get(shortFieldName), fullFieldName);
			}
		}
	}
	//
	/*public static void updatePropsFromJsonCfgFile(final Path propsDir, final RunTimeConfig tgtConfig) {
		ACTION = "update";
		loadPropsFromJsonCfgFile(propsDir, tgtConfig);
	}*/
}
