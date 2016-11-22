package com.emc.mongoose.core.impl.item.base;

import com.emc.mongoose.common.conf.enums.ItemNamingType;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 Created by kurila on 22.11.16.
 */
public class BasicItemNameInputTest {
	
	@Test
	public void testRandomNamingDistribution()
	throws Exception {
		final BasicItemNameInput nameInput = new BasicItemNameInput(
			ItemNamingType.RANDOM, null, 13, 36, 0
		);
		final int N = 100_000_000;
		final Map<Long, Integer> freqMap = new HashMap<>(N);
		
		long nextVal;
		for(int i = 0; i < N; i ++) {
			if(i % (N / 1000) == 0) {
				System.out.print(".");
				System.out.flush();
			}
			nameInput.get();
			nextVal = nameInput.getLastValue();
			if(freqMap.containsKey(nextVal)) {
				freqMap.put(nextVal, freqMap.get(nextVal) + 1);
			} else {
				freqMap.put(nextVal, 1);
			}
		}
		
		long nextFreq;
		for(final long k : freqMap.keySet()) {
			nextFreq = freqMap.get(k);
			if(nextFreq > 1) {
				System.out.println("Number " + k + " occured " + freqMap.get(k) + " times");
			}
		}
	}
}
