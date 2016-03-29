package com.emc.mongoose.common.conf.enums;
//
import com.emc.mongoose.common.generator.FormatGenerator;
//
import java.util.HashMap;
import java.util.Map;
/**
 Created by kurila on 28.03.16.
 */
public enum LoadType {
	//
	WRITE,
	READ,
	DELETE,
	MIXED;
	//
	public static Map<LoadType, Integer> getMixedLoadWeights(final String pattern)
	throws IllegalArgumentException {
		//
		if(pattern == null || pattern.isEmpty()) {
			throw new IllegalArgumentException("Load type pattern is null or empty");
		}
		//
		String p = pattern.toUpperCase();
		final String mixed = LoadType.MIXED.name();
		if(!p.startsWith(mixed)) {
			throw new IllegalArgumentException(
				"Load type pattern should start with \"" + mixed.toLowerCase() + "\" string"
			);
		}
		p = p.substring(mixed.length());
		if(p.charAt(0) != FormatGenerator.FORMAT_SYMBOLS[0]) {
			throw new IllegalArgumentException("Invalid mixed load type pattern");
		}
		if(p.charAt(p.length() - 1) != FormatGenerator.FORMAT_SYMBOLS[1]) {
			throw new IllegalArgumentException("Invalid mixed load type pattern");
		}
		p = p.substring(1, p.length() - 1);
		//
		String tt[];
		LoadType loadType;
		int loadWeight;
		final Map<LoadType, Integer> loadWeights = new HashMap<>();
		for(final String t : p.split(";")) {
			tt = t.split("=");
			if(tt.length != 2) {
				throw new IllegalArgumentException("Invalid mixed load type pattern");
			}
			loadType = LoadType.valueOf(tt[0].toUpperCase());
			loadWeight = Integer.parseInt(tt[1]);
			loadWeights.put(loadType, loadWeight);
		}
		//
		return loadWeights;
	}
}
