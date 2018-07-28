package com.emc.mongoose.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface CliArgUtil {

	String ARG_PREFIX = "--";
	String ARG_PATH_SEP = "-";
	String ARG_VAL_SEP = "=";

	static Map<String, String> parseArgs(final String... args) {
		return Arrays
			.stream(args)
			.map(
				arg -> {
					if(arg.startsWith(ARG_PREFIX)) {
						return arg.substring(ARG_PREFIX.length()).split(ARG_VAL_SEP, 2);
					} else {
						throw new IllegalArgumentNameException(arg);
					}
				}
			)
			.map(
				argValuePair -> argValuePair.length == 2 ?
					argValuePair : new String[] { argValuePair[0], Boolean.TRUE.toString() }
			)
			.collect(Collectors.toMap(argValuePair -> argValuePair[0], argValuePair -> argValuePair[1]));
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
