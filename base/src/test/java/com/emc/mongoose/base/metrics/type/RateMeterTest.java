package com.emc.mongoose.base.metrics.type;

import static org.junit.Assert.assertEquals;

import com.emc.mongoose.base.metrics.snapshot.RateMetricSnapshot;

import java.time.Clock;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/** @author veronika K. on 16.10.18 */
public class RateMeterTest {

	private static final int SLEEP_MILLISEC = 1000;

	@Test
	public void test() throws InterruptedException {
		final RateMeter<RateMetricSnapshot> meter = new RateMeterImpl(Clock.systemUTC(), "SOME_RATE");
		final long t0 = System.currentTimeMillis();
		meter.update(1234);
		TimeUnit.MILLISECONDS.sleep(SLEEP_MILLISEC);
		final long t1 = System.currentTimeMillis();
		final RateMetricSnapshot snapshot1 = meter.snapshot();
		assertEquals(1000.0 * 1234 / (t1 - t0), snapshot1.mean(), 2.0);
		assertEquals(1000.0 * 1234 / (t1 - t0), snapshot1.last(), 2.0);
		meter.update(567);
		TimeUnit.MILLISECONDS.sleep(SLEEP_MILLISEC);
		final long t2 = System.currentTimeMillis();
		final RateMetricSnapshot snapshot2 = meter.snapshot();
		assertEquals(1000.0 * (1234 + 567) / (t2 - t0), snapshot2.mean(), 2.0);
		assertEquals(1000.0 * (1234 + 567) / (t2 - t0), snapshot2.last(), 100.0);
	}
}
