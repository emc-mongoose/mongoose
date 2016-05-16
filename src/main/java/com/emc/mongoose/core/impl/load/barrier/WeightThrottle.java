package com.emc.mongoose.core.impl.load.barrier;
//

import com.emc.mongoose.core.api.load.barrier.Throttle;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

//
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
	private final Map<K, Integer> weightMap; // initial weight map (constant)
	private final Map<K, Integer> remainingWeightMap = new HashMap<>();
	private final AtomicBoolean stopFlag;

	//
	public WeightThrottle(final Map<K, Integer> weightMap, final AtomicBoolean stopFlag)
	throws IllegalArgumentException {
		this.keySet = weightMap.keySet();
		this.weightMap = weightMap;
		this.stopFlag = stopFlag;
		resetRemainingWeights();
	}

	//
	private void resetRemainingWeights()
	throws IllegalArgumentException {
		for(final K key : keySet) {
			remainingWeightMap.put(key, weightMap.get(key));
		}
	}

	//
	@Override
	public final boolean requestContinueFor(final K key)
	throws InterruptedException {
		int remainingWeight;
		while(!stopFlag.get()) {
			synchronized(remainingWeightMap) {
				remainingWeight = remainingWeightMap.get(key);
				if(remainingWeight == 0) {
					for(final K anotherKey : keySet) {
						if(!anotherKey.equals(key)) {
							remainingWeight += remainingWeightMap.get(anotherKey);
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

	//
	@Override
	public final boolean requestContinueFor(final K key, final int times)
	throws InterruptedException {
		int left = times;
		if(left == 0) {
			return true;
		}
		int remainingWeight;
		while(!stopFlag.get()) {
			synchronized(remainingWeightMap) {
				remainingWeight = remainingWeightMap.get(key);
				if(remainingWeight == 0) {
					for(final K anotherKey : keySet) {
						if(!anotherKey.equals(key)) {
							remainingWeight += remainingWeightMap.get(anotherKey);
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
