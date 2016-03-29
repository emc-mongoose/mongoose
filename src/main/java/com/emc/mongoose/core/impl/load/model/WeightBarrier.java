package com.emc.mongoose.core.impl.load.model;
//
import com.emc.mongoose.core.api.load.model.Barrier;
//
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
/**
 Created by kurila on 29.03.16.
 */
public class WeightBarrier<K, T extends Callable<K>>
implements Barrier<T> {
	//
	private final Set<K> keySet;
	private final Map<K, Integer> weightMap;
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
	public final boolean requestApprovalFor(final T thing)
	throws InterruptedException {
		final K key;
		try {
			key = thing.call();
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
	public final boolean requestBatchApprovalFor(
		final List<T> things, final int from, final int to
	) throws InterruptedException {
		int left = to - from;
		if(left == 0) {
			return true;
		}
		final K key;
		try {
			key = things.get(from).call();
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
