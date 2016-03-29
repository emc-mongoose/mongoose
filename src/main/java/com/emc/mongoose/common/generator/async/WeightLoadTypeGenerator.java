package com.emc.mongoose.common.generator.async;
//
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.generator.BasicValueGenerator;
import com.emc.mongoose.common.generator.FormatGenerator;
import com.emc.mongoose.common.math.Random;

import java.util.concurrent.Callable;
/**
 Created by kurila on 28.03.16.
 */
public class WeightLoadTypeGenerator
extends BasicValueGenerator<LoadType> {
	//
	private final static class LoadTypeCalculator
	implements Callable<LoadType> {
		//
		private final LoadType loadTypePerCents[] = new LoadType[100];
		private final Random rnd;
		//
		public LoadTypeCalculator(final String pattern, final Random rnd)
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
			int loadWeight, loadWeightSum = 0;
			for(final String t : p.split(";")) {
				tt = t.split("=");
				if(tt.length != 2) {
					throw new IllegalArgumentException("Invalid mixed load type pattern");
				}
				loadType = LoadType.valueOf(tt[0].toUpperCase());
				loadWeight = Integer.parseInt(tt[1]);
				if(loadWeight > 0 && loadWeight + loadWeightSum <= 100) {
					for(int i = loadWeightSum; i < loadWeightSum + loadWeight; i ++) {
						loadTypePerCents[i] = loadType;
					}
					loadWeightSum += loadWeight;
				}
			}
			//
			if(loadWeightSum != 100) {
				throw new IllegalArgumentException("Invalid mixed load type pattern");
			}
			//
			this.rnd = rnd;
		}
		//
		@Override
		public final LoadType call()
		throws Exception {
			final int i = rnd.nextInt(100);
			return loadTypePerCents[i];
		}
		//
		public LoadType getAnyLoadType() {
			return loadTypePerCents[0];
		}
	}
	//
	public WeightLoadTypeGenerator(final String pattern)
	throws IllegalArgumentException {
		this(new LoadTypeCalculator(pattern, new Random(1L)));
	}
	//
	private WeightLoadTypeGenerator(final LoadTypeCalculator loadTypeCalculator) {
		super(loadTypeCalculator.getAnyLoadType(), loadTypeCalculator);
	}
}
