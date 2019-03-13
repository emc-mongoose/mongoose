package com.emc.mongoose.base.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.github.akurilov.commons.collection.TreeUtil;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.impl.BasicConfig;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface ConfigUtil {

	static ObjectMapper readConfigMapper(final Map<String, Object> schema)
					throws NoSuchMethodException {
		return new YAMLMapper()
						.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
						.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
						.enable(SerializationFeature.INDENT_OUTPUT);
	}

	static ObjectWriter writerWithPrettyPrinter(final ObjectMapper om) {
		final var indenter = (DefaultPrettyPrinter.Indenter) new DefaultIndenter("  ", DefaultIndenter.SYS_LF);
		final var printer = new DefaultPrettyPrinter();
		printer.indentObjectsWith(indenter);
		printer.indentArraysWith(indenter);
		om.enable(SerializationFeature.INDENT_OUTPUT);
		return om.writer(printer);
	}

	static ObjectWriter configWriter() {
		final ObjectMapper mapper = new YAMLMapper();
		return writerWithPrettyPrinter(mapper);
	}

	static Map<String, Object> configTree(final Config config) {
		final var configCopy = (Config) new BasicConfig(config);
		final var configTree = configCopy.mapVal(null);
		for (final var e : configTree.entrySet()) {
			final var key = e.getKey();
			final var val = e.getValue();
			if (val instanceof Config) {
				configTree.replace(key, configTree((Config) val));
			}
		}
		return configTree;
	}

	static String toString(final Config config) {
		try {
			final var configTree = configTree(config);
			return configWriter().writeValueAsString(configTree);
		} catch (final JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	static Config loadConfig(final File file, final Map<String, Object> schema)
					throws NoSuchMethodException, IOException {
		final Map<String, Object> configTree = readConfigMapper(schema)
						.readValue(file, new TypeReference<Map<String, Object>>() {
							{}
						});
		return new BasicConfig("-", schema, configTree);
	}

	static Config loadConfig(final String content, final Map<String, Object> schema)
					throws NoSuchMethodException, IOException {
		final Map<String, Object> configTree = readConfigMapper(schema)
						.readValue(content, new TypeReference<Map<String, Object>>() {
							{}
						});
		return new BasicConfig("-", schema, configTree);
	}

	static Config merge(final String pathSep, final List<Config> configs) {
		final var schema = configs.stream()
						.map(Config::schema)
						.reduce(TreeUtil::addBranches)
						.orElseGet(Collections::emptyMap);
		final var configTree = configs.stream()
						.map(Config::deepToMap)
						.reduce(TreeUtil::addBranches)
						.orElseGet(Collections::emptyMap);
		return new BasicConfig(pathSep, schema, configTree);
	}

	static void flatten(
					final Map<String, Object> configMap,
					final Map<String, String> argValPairs,
					final String sep,
					final String prefix) {
		for (final var k : configMap.keySet()) {
			final var v = configMap.get(k);
			if (v instanceof Map) {
				flatten((Map<String, Object>) v, argValPairs, sep, prefix == null ? k : (prefix + sep + k));
			} else if (v instanceof List) {
				final var s = (String) ((List) v)
								.stream()
								.map(e -> e == null ? ":" : e.toString())
								.collect(Collectors.joining(","));
				argValPairs.put(prefix == null ? k : (prefix + sep + k), s);
			} else {
				argValPairs.put(prefix == null ? k : (prefix + sep + k), v == null ? null : v.toString());
			}
		}
	}
}
