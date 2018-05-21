package com.emc.mongoose.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.impl.BasicConfig;

import com.github.akurilov.confuse.io.json.ConfigJsonDeserializer;
import com.github.akurilov.confuse.io.json.ConfigJsonSerializer;

import java.io.File;
import java.net.URL;
import java.util.Map;

public interface ConfigUtil {

	static ObjectMapper readConfigMapper(final Map<String, Object> schema)
	throws Exception {
		final JsonDeserializer<BasicConfig>
			deserializer = new ConfigJsonDeserializer<>(BasicConfig.class, "-", schema);
		final Module module = new SimpleModule().addDeserializer(BasicConfig.class, deserializer);
		return new ObjectMapper()
			.registerModule(module)
			.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
			.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
			.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
			.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
	}

	static ObjectWriter configWriter() {
		final JsonSerializer<Config> serializer = new ConfigJsonSerializer(Config.class);
		final Module module = new SimpleModule().addSerializer(Config.class, serializer);
		final ObjectMapper mapper = new ObjectMapper()
			.registerModule(module)
			.enable(SerializationFeature.INDENT_OUTPUT);
		final DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter(
			"\t", DefaultIndenter.SYS_LF
		);
		final DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
		printer.indentObjectsWith(indenter);
		printer.indentArraysWith(indenter);
		return mapper.writer(printer);
	}

	static String toString(final Config config) {
		try {
			return configWriter().writeValueAsString(config);
		} catch(final JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	static Config loadConfig(final URL url, final Map<String, Object> schema)
	throws Exception {
		return readConfigMapper(schema).readValue(url, BasicConfig.class);
	}

	static Config loadConfig(final File file, final Map<String, Object> schema)
	throws Exception {
		return readConfigMapper(schema).readValue(file, BasicConfig.class);
	}

}
