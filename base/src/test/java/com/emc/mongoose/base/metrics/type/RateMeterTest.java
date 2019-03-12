package com.emc.mongoose.base.metrics.type;

import static org.junit.Assert.assertEquals;

import com.emc.mongoose.base.metrics.snapshot.RateMetricSnapshot;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/** @author veronika K. on 16.10.18 */
public class RateMeterTest {

	private static final int SLEEP_MILLISEC = 1000;
	private static final int COUNT_BYTES_1 = 1234;
	private static final int COUNT_BYTES_2 = 567;
	private static final double ACCURACY = 0.1;

	@Test
	public void test() throws InterruptedException {
		final RateMeter<RateMetricSnapshot> meter = new RateMeterImpl(Clock.systemUTC(), "SOME_RATE");
		final var t0 = System.currentTimeMillis();
		meter.update(COUNT_BYTES_1);
		TimeUnit.MILLISECONDS.sleep(SLEEP_MILLISEC);
		final var t1 = System.currentTimeMillis();
		final var snapshot1 = meter.snapshot();
		assertEquals(1000.0 * COUNT_BYTES_1 / (t1 - t0), snapshot1.mean(), 2.0);
		assertEquals(1000.0 * COUNT_BYTES_1 / (t1 - t0), snapshot1.last(), 2.0);
		meter.update(COUNT_BYTES_2);
		TimeUnit.MILLISECONDS.sleep(SLEEP_MILLISEC);
		final var t2 = System.currentTimeMillis();
		final var snapshot2 = meter.snapshot();
		final var expectedRate = 1000.0 * (COUNT_BYTES_1 + COUNT_BYTES_2) / (t2 - t0);
		assertEquals(expectedRate, snapshot2.mean(), expectedRate * ACCURACY);
		assertEquals(expectedRate, snapshot2.last(), expectedRate * ACCURACY);
	}
}
