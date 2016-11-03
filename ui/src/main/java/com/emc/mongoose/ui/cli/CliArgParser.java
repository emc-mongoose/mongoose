package com.emc.mongoose.ui.cli;

import java.util.HashMap;
import java.util.Map;

/**
 Created by kurila on 16.08.16.
 */
public final class CliArgParser {
	
	public final static String ARG_PREFIX = "--";
	public final static String ARG_SEP = "-";
	
	public static void main(final String... args)
	throws IllegalArgumentException {
		final Map<String, Object> argTree = parseArgs(args);
		System.out.println(argTree);
	}
	
	public static Map<String, Object> parseArgs(final String... args)
	throws IllegalArgumentException {

		final Map<String, Object> tree = new HashMap<>();

		String argValPair[];
		for(final String arg : args) {
			argValPair = arg.split("=", 2);
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
			final String argParts[] = arg.substring(ARG_PREFIX.length()).split(ARG_SEP);
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
