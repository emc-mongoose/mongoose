package com.emc.mongoose.base.config;

import com.emc.mongoose.base.logging.Loggers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface AliasingUtil {

	String NAME = "name";
	String TARGET = "target";
	String DEPRECATED = "deprecated";
	String ARG_VAL_SEP = "=";

	static Map<String, String> apply(
					final Map<String, String> args, final List<Map<String, Object>> aliasingConfig)
					throws IllegalArgumentException {

		String aliasName;
		String aliasTarget;
		boolean deprecationFlag;
		String[] aliasArgValPair;
		String newArgName;
		String newArgValue;

		final Map<String, String> newArgs = new HashMap<>();

		for (final String argName : args.keySet()) {

			newArgName = argName;
			newArgValue = args.get(argName);

			for (final Map<String, Object> aliasingEntry : aliasingConfig) {
				aliasName = (String) aliasingEntry.get(NAME);
				aliasTarget = (String) aliasingEntry.get(TARGET);
				deprecationFlag = aliasingEntry.containsKey(DEPRECATED) && (boolean) aliasingEntry.get(DEPRECATED);
				if (argName.equals(aliasName)) {
					if (aliasTarget == null) {
						throw new IllegalArgumentException("The argument \"" + aliasName + "\" is deprecated");
					} else if (deprecationFlag) {
						Loggers.ERR.warn(
										"The argument \""
														+ aliasName
														+ "\" is deprecated, use \""
														+ aliasTarget
														+ "\" instead");
					}
					aliasArgValPair = aliasTarget.split(ARG_VAL_SEP, 2);
					newArgName = aliasArgValPair[0];
					if (aliasArgValPair.length == 2) {
						newArgValue = aliasArgValPair[1];
					}
					break;
				}
			}

			newArgs.put(newArgName, newArgValue);
		}

		return newArgs;
	}
}
