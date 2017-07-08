package com.emc.mongoose.tests.perf;

import com.emc.mongoose.common.supply.BatchSupplier;
import com.emc.mongoose.common.supply.RangePatternDefinedSupplier;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 Created by andrey on 06.03.17.
 */
public class RangePatternDefinedSupplierTest {

	private static final int BATCH_SIZE = 0x1000;
	private static final int TIME_LIMIT_SEC = 50;

	@Test
	public void testConstantStringSupplyRate()
	throws Exception {
		final BatchSupplier<String> input = new RangePatternDefinedSupplier("qazxswedc");
		final LongAdder counter = new LongAdder();
		final List<String> buff = new ArrayList<>(BATCH_SIZE);
		final Thread t = new Thread(
			() -> {
				while(true) {
					assertEquals(BATCH_SIZE, input.get(buff, BATCH_SIZE));
					counter.add(BATCH_SIZE);
					buff.clear();
				}
			}
		);
		t.start();
		TimeUnit.SECONDS.timedJoin(t, TIME_LIMIT_SEC);
		t.interrupt();

		System.out.println("Constant string supply rate: " + counter.sum() / TIME_LIMIT_SEC);
	}

	@Test
	public void testSingleLongParamSupplyRate()
	throws Exception {
		final BatchSupplier<String> input = new RangePatternDefinedSupplier("_%d[0-1000]");
		final LongAdder counter = new LongAdder();
		final List<String> buff = new ArrayList<>(BATCH_SIZE);
		final Thread t = new Thread(
			() -> {
				while(true) {
					assertEquals(BATCH_SIZE, input.get(buff, BATCH_SIZE));
					counter.add(BATCH_SIZE);
					buff.clear();
				}
			}
		);
		t.start();
		TimeUnit.SECONDS.timedJoin(t, TIME_LIMIT_SEC);
		t.interrupt();

		System.out.println("Single long parameter supply rate: " + counter.sum() / TIME_LIMIT_SEC);
	}

	@Test
	public void testSingleDoubleParamSupplyRate()
	throws Exception {
		final BatchSupplier<String> input = new RangePatternDefinedSupplier("_%f{#.##}[0-1000]");
		final LongAdder counter = new LongAdder();
		final List<String> buff = new ArrayList<>(BATCH_SIZE);
		final Thread t = new Thread(
			() -> {
				while(true) {
					assertEquals(BATCH_SIZE, input.get(buff, BATCH_SIZE));
					counter.add(BATCH_SIZE);
					buff.clear();
				}
			}
		);
		t.start();
		TimeUnit.SECONDS.timedJoin(t, TIME_LIMIT_SEC);
		t.interrupt();

		System.out.println("Single double parameter supply rate: " + counter.sum() / TIME_LIMIT_SEC);
	}
	
	@Test
	public void testSingleDateParamSupplyRate()
	throws Exception {
		final BatchSupplier<String> input = new RangePatternDefinedSupplier("_%D{yyyy-MM-dd'T'HH:mm:ss.SSS}");
		final LongAdder counter = new LongAdder();
		final List<String> buff = new ArrayList<>(BATCH_SIZE);
		final Thread t = new Thread(
			() -> {
				while(true) {
					assertEquals(BATCH_SIZE, input.get(buff, BATCH_SIZE));
					counter.add(BATCH_SIZE);
					buff.clear();
				}
			}
		);
		t.start();
		TimeUnit.SECONDS.timedJoin(t, TIME_LIMIT_SEC);
		t.interrupt();
		
		System.out.println("Single date parameter supply rate: " + counter.sum() / TIME_LIMIT_SEC);
	}
	
	@Test
	public void testMixedParamsSupplyRate()
	throws Exception {
		final BatchSupplier<String> input = new RangePatternDefinedSupplier("_%d[0-1000]_%f{#.##}[0-1000]_%D{yyyy-MM-dd'T'HH:mm:ss.SSS}");
		final LongAdder counter = new LongAdder();
		final List<String> buff = new ArrayList<>(BATCH_SIZE);
		final Thread t = new Thread(
			() -> {
				while(true) {
					assertEquals(BATCH_SIZE, input.get(buff, BATCH_SIZE));
					counter.add(BATCH_SIZE);
					buff.clear();
				}
			}
		);
		t.start();
		TimeUnit.SECONDS.timedJoin(t, TIME_LIMIT_SEC);
		t.interrupt();
		
		System.out.println("Mixed parameters supply rate: " + counter.sum() / TIME_LIMIT_SEC);
	}
	
	@Test
	public void test10LongParamsSupplyRate()
	throws Exception {
		final BatchSupplier<String> input = new RangePatternDefinedSupplier("_%d[0-10]_%d[10-100]_%d[10-1000]_%d[1000-10000]_%d[10000-100000]_%d[100000-1000000]_%d[1000000-10000000]_%d[10000000-100000000]_%d[100000000-1000000000]_%d[1000000000-2000000000]");
		final LongAdder counter = new LongAdder();
		final List<String> buff = new ArrayList<>(BATCH_SIZE);
		final Thread t = new Thread(
			() -> {
				while(true) {
					assertEquals(BATCH_SIZE, input.get(buff, BATCH_SIZE));
					counter.add(BATCH_SIZE);
					buff.clear();
				}
			}
		);
		t.start();
		TimeUnit.SECONDS.timedJoin(t, TIME_LIMIT_SEC);
		t.interrupt();
		
		System.out.println("10 long parameters supply rate: " + counter.sum() / TIME_LIMIT_SEC);
	}
	
	@Test
	public void test10DoubleParamsSupplyRate()
	throws Exception {
		final BatchSupplier<String> input = new RangePatternDefinedSupplier("_%f{#.###}[0-10]_%f{#.###}[10-100]_%f{#.###}[10-1000]_%f{#.###}[1000-10000]_%f{#.###}[10000-100000]_%f{#.###}[100000-1000000]_%f{#.###}[1000000-10000000]_%f{#.###}[10000000-100000000]_%f{#.###}[100000000-1000000000]_%f{#.###}[1000000000-2000000000]");
		final LongAdder counter = new LongAdder();
		final List<String> buff = new ArrayList<>(BATCH_SIZE);
		final Thread t = new Thread(
			() -> {
				while(true) {
					assertEquals(BATCH_SIZE, input.get(buff, BATCH_SIZE));
					counter.add(BATCH_SIZE);
					buff.clear();
				}
			}
		);
		t.start();
		TimeUnit.SECONDS.timedJoin(t, TIME_LIMIT_SEC);
		t.interrupt();
		
		System.out.println("10 double parameters supply rate: " + counter.sum() / TIME_LIMIT_SEC);
	}
	
	@Test
	public void test10DateParamsSupplyRate()
	throws Exception {
		final BatchSupplier<String> input = new RangePatternDefinedSupplier("_%D{yyyy-MM-dd'T'HH:mm:ss.SSS}_%D{yyyy-MM-dd'T'HH:mm:ss.SSS}_%D{yyyy-'W'ww}_%D{yyMMddHHmmssZ}_%D{EEE, d MMM yyyy HH:mm:ss Z}_%D{yyyyy.MMMMM.dd GGG hh:mm aaa}_%D{K:mm a, z}_%D{h:mm a}_%D{EEE, MMM d, ''yy}_%D{yyyy.MM.dd G 'at' HH:mm:ss z}");
		final LongAdder counter = new LongAdder();
		final List<String> buff = new ArrayList<>(BATCH_SIZE);
		final Thread t = new Thread(
			() -> {
				while(true) {
					assertEquals(BATCH_SIZE, input.get(buff, BATCH_SIZE));
					counter.add(BATCH_SIZE);
					buff.clear();
				}
			}
		);
		t.start();
		TimeUnit.SECONDS.timedJoin(t, TIME_LIMIT_SEC);
		t.interrupt();
		
		System.out.println("10 date parameters supply rate: " + counter.sum() / TIME_LIMIT_SEC);
	}
}
