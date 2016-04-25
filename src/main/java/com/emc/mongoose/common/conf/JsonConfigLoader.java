package com.emc.mongoose.common.conf;
//

import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
//

/**
 * Created by gusakk on 3/13/15.
 */
public class JsonConfigLoader {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	private final AppConfig appConfig;

	//
	public JsonConfigLoader(final AppConfig appConfig) {
		this.appConfig = appConfig;
	}

	//
	public void loadPropsFromJsonCfgFile(final Path filePath) {
		final File cfgFile = filePath.toFile();
		final ObjectMapper jsonMapper = new ObjectMapper()
				.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
				.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
		//
		try {
			JsonNode rootNode;
			if (cfgFile.exists() && cfgFile.isFile()) {
				LOG.debug(Markers.MSG, "Load the config from json file \"{}\"", cfgFile.toString());
				rootNode = jsonMapper.readTree(cfgFile);
			} else {
				final ClassLoader cl = JsonConfigLoader.class.getClassLoader();
				final InputStream bundledConf = cl.getResourceAsStream(AppConfig.FNAME_CONF);
				LOG.debug(
						Markers.MSG, "Load the bundled config", cl.getResource(AppConfig.FNAME_CONF)
				);
				rootNode = jsonMapper.readTree(bundledConf);
			}
			walkJsonTree(rootNode);
		} catch (final IOException e) {
			LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to load properties from \"{}\"", cfgFile
			);
		}
	}

	//
	public void loadPropsFromJsonString(final String string) {
		final ObjectMapper jsonMapper = new ObjectMapper()
				.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
				.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
		//
		try {
			JsonNode rootNode = jsonMapper.readTree(string);
			walkJsonTree(rootNode);

		} catch (IOException e) {
			LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to load properties from \"{}\"", string
			);
		}
	}

	//
	public void loadPropsFromJsonByteArray(final byte buff[]) {
		final ObjectMapper jsonMapper = new ObjectMapper();
		//
		try {
			JsonNode rootNode;
			if (buff != null && buff.length > 0) {
				rootNode = jsonMapper.readTree(buff);
			} else {
				final ClassLoader cl = JsonConfigLoader.class.getClassLoader();
				final InputStream bundledConf = cl.getResourceAsStream(AppConfig.FNAME_CONF);
				LOG.debug(
						Markers.MSG, "Load the bundled config", cl.getResource(AppConfig.FNAME_CONF)
				);
				rootNode = jsonMapper.readTree(bundledConf);
			}
			//
			walkJsonTree(rootNode);
		} catch (final IOException e) {
			LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to load properties from empty byte buffer"
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
		while (fields.hasNext()) {
			final String jsonField = fields.next();
			String propertyName = "";
			if (fieldPrefix != null) {
				if (fieldPrefix.isEmpty()) {
					propertyName = jsonField;
				} else {
					propertyName = fieldPrefix + Constants.DOT + jsonField;
				}
			}

			// load configuration from mongoose.json
			final JsonNode nodeValue = jsonNode.get(jsonField);
			if (!propertyName.startsWith(AppConfig.PREFIX_KEY_ALIASING)) {
				LOG.trace(
						Markers.MSG, "Read property: \"{}\" = {}", propertyName, nodeValue
				);
			}
			//
			if (!nodeValue.isNull()) {
				switch (nodeValue.getNodeType()) {
					case ARRAY:
						final Iterator<JsonNode> i = nodeValue.elements();
						final StringBuilder strb = new StringBuilder();
						while (i.hasNext()) {
							strb.append(i.next().asText()).append(',');
						}
						strb.setLength(strb.length() - 1); // remove last comma
						appConfig.setProperty(propertyName, strb.toString());
						break;
					case BINARY:
						appConfig.setProperty(propertyName, nodeValue.asText());
						break;
					case BOOLEAN:
						appConfig.setProperty(propertyName, nodeValue.asBoolean());
						break;
					case MISSING:
						throw new IllegalStateException(
								"No such value \"" + propertyName + "\""
						);
					case NULL:
						appConfig.setProperty(propertyName, null);
						break;
					case NUMBER:
						if (nodeValue.isDouble() || nodeValue.isFloat()) {
							appConfig.setProperty(propertyName, nodeValue.asDouble());
						} else if (nodeValue.isLong() || nodeValue.isInt()) {
							appConfig.setProperty(propertyName, nodeValue.asLong());
						} else if (nodeValue.isBigDecimal()) {
							appConfig.setProperty(propertyName, nodeValue.asText());
						} else {
							throw new IllegalStateException(
									"Unexpected value type of \"" + propertyName + "\""
							);
						}
						break;
					case OBJECT:
						walkJsonTree(nodeValue, propertyName);
						break;
					case POJO:
						throw new IllegalStateException(
								"Unsupported value of \"" + propertyName + "\""
						);
					case STRING:
						appConfig.setProperty(propertyName, nodeValue.asText());
						break;
				}
			} else {
				appConfig.setProperty(propertyName, null);
			}
		}
	}

	//
	public void updateJsonCfgFile(final File cfgFile) {
		LOG.debug("Going to update the configuration file \"{}\"", cfgFile.toString());
		try (
				final BufferedWriter
						writer = Files.newBufferedWriter(
						cfgFile.toPath(), StandardCharsets.UTF_8, StandardOpenOption.WRITE
				)
		) {
			writer.write(appConfig.toFormattedString());
		} catch (final IOException e) {
			LogUtil.exception(
					LOG, Level.WARN, e, "Failed to update properties in \"{}\"", cfgFile);
		}
	}
	/*
	private static String getFormattedValue(final String value) {
		return value
			.replace("[", "")
			.replace("]", "")
			.replace(" ", "")
			.replace("\"", "")
			.trim();
	}*/
}
