package com.emc.mongoose.cli;

import com.emc.mongoose.config.IllegalArgumentNameException;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public interface CliArgUtil {

	String ARG_PREFIX = "--";
	String ARG_VAL_SEP = "=";

	static Map<String, String> parseArgs(final String... args) {
		return Arrays
			.stream(args)
			.map(
				arg -> {
					if(arg.startsWith(ARG_PREFIX)) {
						return arg
							.substring(ARG_PREFIX.length())
							.split(ARG_VAL_SEP, 2);
					} else {
						throw new IllegalArgumentNameException(arg);
					}
				}
			)
			.map(
				argValuePair -> argValuePair.length == 2 ?
					argValuePair : new String[] { argValuePair[0], Boolean.TRUE.toString() }
			)
			.collect(
				Collectors.toMap(
					argValuePair -> argValuePair[0],
					argValuePair -> argValuePair[1]
				)
			);
	}
}
