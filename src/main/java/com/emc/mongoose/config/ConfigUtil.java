package com.emc.mongoose.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
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
import com.github.akurilov.confuse.io.json.TypeJsonDeserializer;

import java.io.File;
import java.net.URL;
import java.util.Map;

public interface ConfigUtil {

	static ObjectMapper readConfigMapper(final Map<String, Object> schema)
	throws Exception {
		final JsonDeserializer<? extends Config>
			deserializer = new ConfigJsonDeserializer<>(BasicConfig.class, "-", schema);
		final Module module = new SimpleModule().addDeserializer(Config.class, deserializer);
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

	static Config loadConfig(final URL url, final Map<String, Object> schema)
	throws Exception {
		return readConfigMapper(schema).readValue(url, BasicConfig.class);
	}

	static Config loadConfig(final File file, final Map<String, Object> schema)
	throws Exception {
		return readConfigMapper(schema).readValue(file, BasicConfig.class);
	}

	static ObjectMapper readSchemaMapper() {
		final JsonDeserializer deserializer = new TypeJsonDeserializer(String.class);
		final Module module = new SimpleModule().addDeserializer(String.class, deserializer);
		return new ObjectMapper()
			.registerModule(module)
			.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
			.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
			.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
			.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
	}

	static Map<String, Object> loadConfigSchema(final URL url)
	throws Exception {
		final TypeReference<Map<String, Object>>
			typeRef = new TypeReference<Map<String, Object>>() {};
		return readSchemaMapper().readValue(url, typeRef);
	}
}
