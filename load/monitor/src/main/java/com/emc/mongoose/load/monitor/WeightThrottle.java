package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.concurrent.Throttle;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.Set;

/**
 Created by kurila on 29.03.16.
 A kind of very abstract throttle which uses the map of weights.
 Each thing (subject) should be an instance having a key for the weights map.
 The throttle determines the weight for each thing and makes the decision.
 The weight is used to pass the things with specific ratio for the different keys.
 */
public final class WeightThrottle<K>
implements Throttle<K> {

	private final Set<K> weightKeys; // just to not to calculate every time
	private final Object2IntMap<K> weightMap; // initial weight map (constant)
	private final Object2IntMap<K> remainingWeightMap = new Object2IntOpenHashMap<>();

	public WeightThrottle(final Object2IntMap<K> weightMap)
	throws IllegalArgumentException {
		this.weightKeys = weightMap.keySet();
		this.weightMap = weightMap;
		resetRemainingWeights();
	}

	private void resetRemainingWeights()
	throws IllegalArgumentException {
		for(final K key : weightKeys) {
			remainingWeightMap.put(key, weightMap.getInt(key));
		}
	}

	private void ensureRemainingWeights() {
		for(final K key : weightKeys) {
			if(remainingWeightMap.getInt(key) > 0) {
				return;
			}
		}
		for(final K key : weightKeys) {
			remainingWeightMap.put(key, weightMap.getInt(key));
		}
	}

	@Override
	public final boolean getPassFor(final K key) {
		synchronized(remainingWeightMap) {
			ensureRemainingWeights();
			final int remainingWeight = remainingWeightMap.getInt(key);
			if(remainingWeight > 0) {
				remainingWeightMap.put(key ,remainingWeight - 1);
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public final int getPassFor(final K key, final int times) {
		if(times == 0) {
			return 0;
		}
		synchronized(remainingWeightMap) {
			ensureRemainingWeights();
			final int remainingWeight = remainingWeightMap.getInt(key);
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
