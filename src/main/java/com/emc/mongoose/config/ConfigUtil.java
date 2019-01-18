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
import com.github.akurilov.commons.collection.TreeUtil;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.impl.BasicConfig;
import com.github.akurilov.confuse.io.json.ConfigJsonDeserializer;
import com.github.akurilov.confuse.io.json.ConfigJsonSerializer;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface ConfigUtil {

  static ObjectMapper readConfigMapper(final Map<String, Object> schema)
      throws NoSuchMethodException {
    final JsonDeserializer<BasicConfig> deserializer =
        new ConfigJsonDeserializer<>(BasicConfig.class, "-", schema);
    final Module module = new SimpleModule().addDeserializer(BasicConfig.class, deserializer);
    return new ObjectMapper()
        .registerModule(module)
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
        .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
        .configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
  }

  public static ObjectWriter writerWithPrettyPrinter(final ObjectMapper om) {
    final DefaultPrettyPrinter.Indenter indenter =
        new DefaultIndenter("  ", DefaultIndenter.SYS_LF);
    final DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
    printer.indentObjectsWith(indenter);
    printer.indentArraysWith(indenter);
    om.enable(SerializationFeature.INDENT_OUTPUT);
    return om.writer(printer);
  }

  static ObjectWriter configWriter() {
    final JsonSerializer<Config> serializer = new ConfigJsonSerializer(Config.class);
    final Module module = new SimpleModule().addSerializer(Config.class, serializer);
    final ObjectMapper mapper = new ObjectMapper().registerModule(module);
    return writerWithPrettyPrinter(mapper);
  }

  static String toString(final Config config) {
    try {
      return configWriter().writeValueAsString(config);
    } catch (final JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  static Config loadConfig(final File file, final Map<String, Object> schema)
      throws NoSuchMethodException, IOException {
    return readConfigMapper(schema).readValue(file, BasicConfig.class);
  }

  static Config loadConfig(final String content, final Map<String, Object> schema)
      throws NoSuchMethodException, IOException {
    return readConfigMapper(schema).readValue(content, BasicConfig.class);
  }

  static Config merge(final String pathSep, final List<Config> configs) {
    final Map<String, Object> schema =
        configs.stream()
            .map(Config::schema)
            .reduce(TreeUtil::addBranches)
            .orElseGet(Collections::emptyMap);
    final Map<String, Object> configTree =
        configs.stream()
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
    for (final String k : configMap.keySet()) {
      final Object v = configMap.get(k);
      if (v instanceof Map) {
        flatten((Map<String, Object>) v, argValPairs, sep, prefix == null ? k : (prefix + sep + k));
      } else if (v instanceof List) {
        final String s =
            (String)
                ((List) v)
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
