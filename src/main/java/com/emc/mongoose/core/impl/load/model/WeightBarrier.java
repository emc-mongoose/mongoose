package com.emc.mongoose.core.impl.load.model;
//
import com.emc.mongoose.core.api.load.model.Barrier;
//
import java.util.HashMap;
import java.util.List;
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
public class WeightBarrier<K, T extends Map.Entry<K, ?>>
implements Barrier<T> {

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
	public final boolean getApprovalFor(final T thing)
	throws InterruptedException {
		final K key;
		try {
			key = thing.getKey();
		} catch(final Exception e) {
			return false;
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
						remainingWeightMap.wait();
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
	public final boolean getBatchApprovalFor(
		final List<T> things, final int from, final int to
	) throws InterruptedException {
		int left = to - from;
		if(left == 0) {
			return true;
		}
		final K key;
		try {
			key = things.get(from).getKey();
		} catch(final Exception e) {
			return false;
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
						remainingWeightMap.wait();
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
