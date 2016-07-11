package com.emc.mongoose.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 Created by kurila on 11.07.16.
 */
public enum LoadType {
	//
	CREATE,
	READ,
	UPDATE,
	DELETE;
	//
	public static Map<LoadType, Integer> getMixedLoadWeights(final List<String> patterns)
	throws IllegalArgumentException {
		//
		if(patterns == null || patterns.size() < 2) {
			throw new IllegalArgumentException("Load type patterns is null/empty/single");
		}
		//
		String parts[], tailPart;
		LoadType loadType;
		int loadWeight;
		// use LinkedHashMap to save the order
		final Map<LoadType, Integer> loadWeights = new LinkedHashMap<>();
		for(final String pattern : patterns) {
			parts = pattern.split("=");
			if(parts.length != 2) {
				throw new IllegalArgumentException("Invalid pattern: \"" + pattern + "\"");
			}
			loadType = LoadType.valueOf(parts[0].toUpperCase());
			tailPart = parts[1];
			if(tailPart.endsWith("%")) {
				loadWeight = Integer.parseInt(tailPart.substring(0, tailPart.length() - 1));
			} else {
				loadWeight = Integer.parseInt(tailPart);
			}
			loadWeights.put(loadType, loadWeight);
		}
		//
		return loadWeights;
	}
}
