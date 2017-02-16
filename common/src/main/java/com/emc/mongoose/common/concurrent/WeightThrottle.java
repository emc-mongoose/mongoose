package com.emc.mongoose.common.concurrent;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 Created by kurila on 29.03.16.
 An I/O task throttle which uses the map of weights.
 The throttle determines the weight for each I/O task and makes the decision.
 The weight is used to pass the I/O task with specific ratio for the different keys.
 */
public final class WeightThrottle {

	// just to not to calculate every time
	private final IntSet weightKeys = new IntArraySet();
	// initial weight map (constant)
	private final Int2IntMap weightMap = new Int2IntOpenHashMap();
	private final Int2IntMap remainingWeightMap = new Int2IntOpenHashMap();

	public WeightThrottle(final Int2IntMap weightMap)
	throws IllegalArgumentException {
		this.weightKeys.addAll(weightMap.keySet());
		this.weightMap.putAll(weightMap);
		resetRemainingWeights();
	}

	private void resetRemainingWeights()
	throws IllegalArgumentException {
		for(final int key : weightKeys) {
			remainingWeightMap.put(key, weightMap.get(key));
		}
	}

	private void ensureRemainingWeights() {
		for(final int key : weightKeys) {
			if(remainingWeightMap.get(key) > 0) {
				return;
			}
		}
		resetRemainingWeights();
	}

	public final boolean tryAcquire(final int key) {
		synchronized(remainingWeightMap) {
			ensureRemainingWeights();
			final int remainingWeight = remainingWeightMap.get(key);
			if(remainingWeight > 0) {
				remainingWeightMap.put(key, remainingWeight - 1);
				return true;
			} else {
				return false;
			}
		}
	}

	public final int tryAcquire(final int key, final int times) {
		if(times == 0) {
			return 0;
		}
		synchronized(remainingWeightMap) {
			ensureRemainingWeights();
			final int remainingWeight = remainingWeightMap.get(key);
			if(times > remainingWeight) {
				remainingWeightMap.put(key, 0);
				return remainingWeight;
			} else {
				remainingWeightMap.put(key, remainingWeight - times);
				return times;
			}
		}
	}
}
