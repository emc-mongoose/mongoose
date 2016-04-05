package com.emc.mongoose.core.impl.load.barrier;
//
import com.emc.mongoose.core.api.load.barrier.Barrier;
//
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 Created by kurila on 29.03.16.
 A kind of very abstract barrier which uses the map of weights.
 Each thing (subject) should be an instance having a key for the weights map.
 The barrier determines the weight for each thing and makes the decision.
 The weight is used to pass the things with specific ratio for the different keys.
 This implementation of the barrier never denies the things but blocks until a thing may be passed.
 */
public class WeightBarrier<K>
implements Barrier<K> {

	private final Set<K> keySet; // just to not to calculate every time
	private final Map<K, Integer> weightMap; // initial weight map (constant)
	private final Map<K, Integer> remainingWeightMap = new HashMap<>();

	//
	public WeightBarrier(final Map<K, Integer> weightMap)
	throws IllegalArgumentException {
		this.keySet = weightMap.keySet();
		this.weightMap = weightMap;
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
	public final boolean getApprovalFor(final K key)
	throws InterruptedException {
		int remainingWeight;
		while(true) {
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
	public final boolean getApprovalsFor(final K key, final int times)
	throws InterruptedException {
		int left = times;
		if(left == 0) {
			return true;
		}
		int remainingWeight;
		while(true) {
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
