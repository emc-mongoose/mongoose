package com.emc.mongoose.tests.perf;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.pattern.RangePatternDefinedInput;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static com.emc.mongoose.common.Constants.BATCH_SIZE;
import static org.junit.Assert.fail;

/**
 Created by andrey on 06.03.17.
 */
public class RangePatternDefinedInputTest {

	private static final int TIME_LIMIT_SEC = 10;

	@Test
	public void testConstantInputRate()
	throws Exception {
		final Input<String> input = new RangePatternDefinedInput("qazxswedc");
		final LongAdder counter = new LongAdder();
		final List<String> buff = new ArrayList<>(BATCH_SIZE);
		final Thread t = new Thread(
			() -> {
				try {
					while(true) {
						assertEquals(BATCH_SIZE, input.get(buff, BATCH_SIZE));
						counter.add(BATCH_SIZE);
						buff.clear();
					}
				} catch(final IOException e) {
					fail(e.toString());
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
		final Input<String> input = new RangePatternDefinedInput("qazxsw%d[0-1000]");
		final LongAdder counter = new LongAdder();
		final List<String> buff = new ArrayList<>(BATCH_SIZE);
		final Thread t = new Thread(
			() -> {
				try {
					while(true) {
						assertEquals(BATCH_SIZE, input.get(buff, BATCH_SIZE));
						counter.add(BATCH_SIZE);
						buff.clear();
					}
				} catch(final IOException e) {
					fail(e.toString());
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
		final Input<String> input = new RangePatternDefinedInput("qazxsw%f{#.##}[0-1000]");
		final LongAdder counter = new LongAdder();
		final List<String> buff = new ArrayList<>(BATCH_SIZE);
		final Thread t = new Thread(
			() -> {
				try {
					while(true) {
						assertEquals(BATCH_SIZE, input.get(buff, BATCH_SIZE));
						counter.add(BATCH_SIZE);
						buff.clear();
					}
				} catch(final IOException e) {
					fail(e.toString());
				}
			}
		);
		t.start();
		TimeUnit.SECONDS.timedJoin(t, TIME_LIMIT_SEC);
		t.interrupt();

		System.out.println("Constant input rate: " + counter.sum() / TIME_LIMIT_SEC);
	}
}
