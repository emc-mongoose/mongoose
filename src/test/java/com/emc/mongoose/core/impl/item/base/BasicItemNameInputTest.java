package com.emc.mongoose.core.impl.item.base;

import com.emc.mongoose.common.conf.enums.ItemNamingType;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.fail;

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
		final int N = 1_000_000_000;
		final Map<Long, Integer> freqMap = new HashMap<>(N);
		
		long nextVal;
		for(int i = 0; i < N; i ++) {
			if(i % (N / 100) == 0) {
				System.out.println(i + " subsequent IDs are unique");
			}
			nameInput.get();
			nextVal = nameInput.getLastValue();
			if(freqMap.containsKey(nextVal)) {
				fail(Long.toString(nextVal, Character.MAX_RADIX));
			} else {
				freqMap.put(nextVal, 1);
			}
		}
	}
}
