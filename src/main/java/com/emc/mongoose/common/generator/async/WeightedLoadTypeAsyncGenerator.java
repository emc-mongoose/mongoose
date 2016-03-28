package com.emc.mongoose.common.generator.async;
//
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.generator.FormatGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
/**
 Created by kurila on 28.03.16.
 */
public class WeightedLoadTypeAsyncGenerator<T extends LoadType>
extends AsyncValueGenerator<T> {
	//
	private final static class LoadTypeCalculator<T extends LoadType>
	implements InitCallable<T> {
		//
		private final Map<LoadType, Short> loadTypeWeightMap = new HashMap<>();
		//
		public LoadTypeCalculator(final String pattern)
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
			short loadWeight, loadWeightSum = 0;
			for(final String t : p.split(";")) {
				tt = t.split("=");
				if(tt.length != 2) {
					throw new IllegalArgumentException("Invalid mixed load type pattern");
				}
				loadType = LoadType.valueOf(tt[0].toUpperCase());
				loadWeight = Short.parseShort(tt[1]);
				if(loadWeight > 0 && loadWeight + loadWeightSum <= 100) {
					loadTypeWeightMap.put(loadType, loadWeight);
					loadWeightSum += loadWeight;
				}
			}
			//
			if(loadWeightSum != 100) {
				throw new IllegalArgumentException("Invalid mixed load type pattern");
			}
		}
		//
		@Override
		public final T call() throws Exception {
			return null;
		}
		//
		@Override
		public final boolean isInitialized() {
			return true;
		}
	}
	//
	public WeightedLoadTypeAsyncGenerator(final String pattern)
	throws IllegalArgumentException {
		super(null, new LoadTypeCalculator<>(pattern));
	}
	//
}
