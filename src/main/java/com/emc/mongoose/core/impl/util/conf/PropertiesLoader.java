package com.emc.mongoose.core.impl.util.conf;

import com.emc.mongoose.core.api.util.log.Markers;
import com.emc.mongoose.core.impl.util.RunTimeConfig;
import com.emc.mongoose.core.impl.util.log.TraceLogger;
import com.emc.mongoose.run.Main;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by gusakk on 3/13/15.
 */
public class PropertiesLoader {
	private final static Logger LOG = LogManager.getLogger();
	private final static Set<String> mongooseKeys = new HashSet<>();
	//
	private static RunTimeConfig DEFAULT_CFG;
	private static PropertiesLoaderActions ACTION = PropertiesLoaderActions.LOAD;
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
			tgtConfig.setMongooseKeys(mongooseKeys);
			tgtConfig.putJsonProps(rootNode);
			//
			if (ACTION.equals(PropertiesLoaderActions.UPLOAD)) {
				jsonMapper.writerWithDefaultPrettyPrinter().writeValue(cfgFile, rootNode);
			}
		} catch (IOException e) {
			TraceLogger.failure(LOG, Level.ERROR, e,
					String.format("Failed to load properties from \"%s\"", cfgFile.toString()));
		}
	}
	//
	private static void walkJsonTree(final JsonNode jsonNode) {
		final List<DefaultMapEntry<String, Object>> props = new ArrayList<>();
		walkJsonTree(jsonNode, props, null);
	}
	//
	private static void walkJsonTree(final JsonNode jsonNode, List<DefaultMapEntry<String, Object>> props,
	                                 final String fullFileName) {
		final Iterator<String> fields = jsonNode.fieldNames();
		while (fields.hasNext()) {
			final String shortFieldName = fields.next();
			String fullFieldName = "";
			if (fullFileName != null) {
				if (fullFileName.isEmpty()) {
					fullFieldName = shortFieldName;
				} else {
					fullFieldName = fullFileName + Main.DOT + shortFieldName;
				}
			}
			if (!jsonNode.get(shortFieldName).fieldNames().hasNext()) {
				if (ACTION.equals(PropertiesLoaderActions.UPDATE)
						|| ACTION.equals(PropertiesLoaderActions.UPLOAD)) {
					final String property = DEFAULT_CFG.getProperty(fullFieldName).toString()
							.replace("[", "")
							.replace("]", "")
							.replace(" ", "")
							.trim();
					DEFAULT_CFG.setProperty(fullFieldName, DEFAULT_CFG.getProperty(fullFieldName));
					((ObjectNode) jsonNode).put(shortFieldName, property);
				} else {
					DEFAULT_CFG.setProperty(fullFieldName, jsonNode.get(shortFieldName).toString()
							.replace("\"", "")
							.trim());
					mongooseKeys.add(fullFieldName);
				}
			} else {
				props = new ArrayList<>();
				walkJsonTree(jsonNode.get(shortFieldName), props, fullFieldName);
			}
		}
	}
	//
	public static void updateProps(final Path rootDir, final RunTimeConfig tgtConfig, final boolean isUpload) {
		if (isUpload) {
			ACTION = PropertiesLoaderActions.UPLOAD;
		} else {
			ACTION = PropertiesLoaderActions.UPDATE;
		}
		loadPropsFromJsonCfgFile(rootDir, tgtConfig);
	}
}
