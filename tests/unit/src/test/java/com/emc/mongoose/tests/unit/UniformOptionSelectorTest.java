package com.emc.mongoose.tests.unit;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.UniformOptionSelector;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 Created by kurila on 03.10.16.
 */
public class UniformOptionSelectorTest {
	
	@Test
	public void testBalancing()
	throws Exception {
		final String options[] = new String[] { "a", "b", "c", "d" };
		final Input<String> balancer = new UniformOptionSelector<>(options);
		final int N = 1000;
		String r;
		int a = 0, b = 0, c = 0, d = 0;
		for(int i = 0; i < N * options.length; i ++) {
			r = balancer.get();
			switch(r) {
				case "a":
					a ++;
					break;
				case "b":
					b ++;
					break;
				case "c":
					c ++;
					break;
				case "d":
					d ++;
					break;
			}
		}
		assertEquals(a, N);
		assertEquals(b, N);
		assertEquals(c, N);
		assertEquals(d, N);
	}
	
	@Test
	public void testBalancingBatch()
	throws Exception {
		final String options[] = new String[] { "A", "B", "C" };
		final Input<String> balancer = new UniformOptionSelector<>(options);
		final int N = 100000;
		final List<String> buff = new ArrayList<>(256);
		int A = 0, B = 0, C = 0;
		for(int i = 0; i < N * options.length; i ++) {
			final int n = balancer.get(buff, 256);
			for(int j = 0; j < n; j ++) {
				switch(buff.get(i)) {
					case "A":
						A ++;
						break;
					case "B":
						B ++;
						break;
					case "C":
						C ++;
						break;
				}
			}
		}
		assertEquals(((double) A) / B, ((double) B) / C, 0.01);
	}
}
