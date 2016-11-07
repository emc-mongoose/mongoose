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
 This implementation of the barrier never denies the things but blocks until a thing may be passed.
 */
public class WeightThrottle<K>
implements Throttle<K> {

	private final Set<K> keySet; // just to not to calculate every time
	private final Object2IntMap<K> weightMap; // initial weight map (constant)
	private final Object2IntMap<K> remainingWeightMap = new Object2IntOpenHashMap<>();

	public WeightThrottle(final Object2IntMap<K> weightMap)
	throws IllegalArgumentException {
		this.keySet = weightMap.keySet();
		this.weightMap = weightMap;
		resetRemainingWeights();
	}

	private void resetRemainingWeights()
	throws IllegalArgumentException {
		for(final K key : keySet) {
			remainingWeightMap.put(key, weightMap.get(key));
		}
	}

	@Override
	public final boolean getPassFor(final K key)
	throws InterruptedException {
		int remainingWeight;
		while(true) {
			synchronized(remainingWeightMap) {
				remainingWeight = remainingWeightMap.get(key);
				if(remainingWeight == 0) {
					for(final K anotherKey : keySet) {
						if(!anotherKey.equals(key)) {
							remainingWeight += remainingWeightMap.getInt(anotherKey);
						}
					}
					if(remainingWeight == 0) {
						resetRemainingWeights();
						remainingWeightMap.notify();
					} else {
						remainingWeightMap.wait(1000);
					}
				} else { // remaining weight is more than 0
					remainingWeightMap.put(key, remainingWeight - 1);
					break;
				}
			}
		}
		return true;
	}

	@Override
	public final boolean getPassFor(final K key, final int times)
	throws InterruptedException {
		int left = times;
		if(left == 0) {
			return true;
		}
		int remainingWeight;
		while(true) {
			synchronized(remainingWeightMap) {
				remainingWeight = remainingWeightMap.getInt(key);
				if(remainingWeight == 0) {
					for(final K anotherKey : keySet) {
						if(!anotherKey.equals(key)) {
							remainingWeight += remainingWeightMap.getInt(anotherKey);
						}
					}
					if(remainingWeight == 0) {
						resetRemainingWeights();
						remainingWeightMap.notify();
					} else {
						remainingWeightMap.wait(1);
					}
				} else if(remainingWeight < left) {
					remainingWeightMap.put(key, 0);
					left -= remainingWeight;
				} else {
					remainingWeightMap.put(key, remainingWeight - left);
					break;
				}
			}
		}
		return true;
	}
}
