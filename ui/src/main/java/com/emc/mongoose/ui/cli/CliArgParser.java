package com.emc.mongoose.ui.cli;

import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.Markers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 Created by kurila on 16.08.16.
 */
public final class CliArgParser {
	
	private static final Logger LOG = LogManager.getLogger();

	public static final String ARG_PREFIX = "--";

	public static void main(final String... args)
	throws IllegalArgumentException {
		final Map<String, Object> argTree = parseArgs(null, args);
		System.out.println(argTree);
	}
	
	public static Map<String, Object> parseArgs(
		final List<Map<String, Object>> aliasingConfig, final String... args
	) throws IllegalArgumentException {

		final Map<String, Object> tree = new HashMap<>();

		String argValPair[];
		String aliasArgValPair[];
		String nextAliasName;
		String nextAliasTarget;
		boolean nextDeprecatedFlag;

		for(final String arg : args) {

			argValPair = arg.split("=", 2);

			if(aliasingConfig != null) {
				for(final Map<String, Object> aliasingNode : aliasingConfig) {
					nextAliasName = ARG_PREFIX + aliasingNode.get(Config.KEY_NAME);
					nextAliasTarget = ARG_PREFIX + aliasingNode.get(Config.KEY_TARGET);
					nextDeprecatedFlag = aliasingNode.containsKey(Config.KEY_DEPRECATED) ?
										 (boolean) aliasingNode.get(Config.KEY_DEPRECATED) : false;
					if(arg.startsWith(nextAliasName)) {
						if(nextDeprecatedFlag) {
							LOG.warn(
								Markers.ERR,
								"The argument \"{}\" is deprecated, use \"{}\" instead",
								nextAliasName, nextAliasTarget
							);
						}
						aliasArgValPair = nextAliasTarget.split("=", 2);
						argValPair[0] = aliasArgValPair[0];
						if(aliasArgValPair.length == 2) {
							if(argValPair.length == 2) {
								argValPair[1] = aliasArgValPair[1];
							} else {
								argValPair = new String[] { argValPair[0], aliasArgValPair[1] };
							}
						}
						break;
					}
				}
			}

			if(argValPair.length > 1) {
				parseArg(tree, argValPair[0], argValPair[1]);
			} else {
				parseArg(tree, argValPair[0]);
			}
		}

		return tree;
	}
	
	private static void parseArg(
		final Map<String, Object> tree, final String arg, final String value
	) throws IllegalArgumentException {
		if(arg.startsWith(ARG_PREFIX) && arg.length() > ARG_PREFIX.length()) {
			final String argParts[] = arg.substring(ARG_PREFIX.length()).split(Config.PATH_SEP);
			Map<String, Object> subTree = tree;
			String argPart;
			for(int i = 0; i < argParts.length; i ++) {
				argPart = argParts[i];
				if(i < argParts.length - 1) {
					Object node = subTree.get(argPart);
					if(node == null) {
						node = new HashMap<>();
						subTree.put(argPart, node);
					}
					subTree = (Map<String, Object>) node;
				} else { // last part
					subTree.put(argPart, value);
				}
			}
		} else {
			throw new IllegalArgumentException(arg);
		}
	}
	
	private static void parseArg(final Map<String, Object> tree, final String arg) {
		parseArg(tree, arg, Boolean.TRUE.toString());
	}
}
