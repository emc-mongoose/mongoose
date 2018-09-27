package com.emc.mongoose.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;

public interface CliArgUtil {

	String ARG_PREFIX = "--";
	String ARG_PATH_SEP = "-";
	String ARG_VAL_SEP = "=";
	String NESTED_ARG_VAL_SEP = ":";

	static Map<String, Object> parseArgs(final String... args) {
		return Arrays
			.stream(args)
			.peek(
				arg -> {
					if(!arg.startsWith(ARG_PREFIX)) {
						throw new IllegalArgumentNameException(arg);
					}
				}
			)
			.map(arg -> arg.substring(ARG_PREFIX.length()))
			// split args to key/value pairs by the '=' symbol
			.map(arg -> arg.split(ARG_VAL_SEP, 2))
			// handle the shortcuts for boolean options (--smth-enabled -> --smth-enabled=true)
			.map(argValPair -> argValPair.length == 2 ? argValPair : new String[] { argValPair[0], TRUE.toString() })
			// handle the "nested" arg/val pairs in the values, merge them into the map
			.collect(
				Collectors.toMap(
					// key
					argValPair -> argValPair[0],
					// value
					argValPair -> {
						final String[] nestedArgValPair = argValPair[1].split(NESTED_ARG_VAL_SEP, 2);
						if(nestedArgValPair.length == 1) {
							return argValPair[1];
						} else {
							final Map<String, String> result = new HashMap<>();
							result.put(nestedArgValPair[0], nestedArgValPair[1]);
							return result;
						}
					},
					// merge the values under the same key properly
					(val1, val2) -> {
						((Map<String, String>) val1).putAll((Map<String, String>) val2);
						return val1;
					}
				)
			);
	}

	@SuppressWarnings("CollectionWithoutInitialCapacity")
	static List<String> allCliArgs(final Map<String, Object> schema, final String sep) {
		final List<String> allArgs = new ArrayList<>();
		schema
			.entrySet()
			.stream()
			.map(schemaEntry -> argsFromSchemaEntry(ARG_PREFIX, sep, schemaEntry))
			.forEach(allArgs::addAll);
		return allArgs;
	}

	@SuppressWarnings({ "CollectionWithoutInitialCapacity", "unchecked" })
	static List<String> argsFromSchemaEntry(
		final String prefix, final String sep, final Map.Entry<String, Object> schemaEntry
	) {
		final List<String> args = new ArrayList<>();
		final String schemaKey = schemaEntry.getKey();
		final Object schemaVal = schemaEntry.getValue();
		if(schemaVal instanceof Map) {
			((Map<String, Object>) schemaVal)
				.entrySet()
				.stream()
				.map(e -> argsFromSchemaEntry(prefix + schemaKey + sep, sep, e))
				.forEach(args::addAll);
		} else {
			args.add(prefix + schemaKey + ARG_VAL_SEP + '<' + schemaVal + '>');
		}
		return args;
	}
}
