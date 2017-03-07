package com.emc.mongoose.tests.perf;

import com.emc.mongoose.common.supply.BatchSupplier;
import com.emc.mongoose.common.supply.RangePatternDefinedSupplier;
import static com.emc.mongoose.common.Constants.BATCH_SIZE;

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

	private static final int TIME_LIMIT_SEC = 10;

	@Test
	public void testConstantInputRate()
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

		System.out.println("Constant input rate: " + counter.sum() / TIME_LIMIT_SEC);
	}

	@Test
	public void testSingleIntInputRate()
	throws Exception {
		final BatchSupplier<String> input = new RangePatternDefinedSupplier("qazxsw%d[0-1000]");
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

		System.out.println("Constant input rate: " + counter.sum() / TIME_LIMIT_SEC);
	}

	@Test
	public void testSingleFloatInputRate()
	throws Exception {
		final BatchSupplier<String> input = new RangePatternDefinedSupplier("qazxsw%f{#.##}[0-1000]");
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

		System.out.println("Constant input rate: " + counter.sum() / TIME_LIMIT_SEC);
	}
}
