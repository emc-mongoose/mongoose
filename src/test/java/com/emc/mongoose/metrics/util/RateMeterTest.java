package com.emc.mongoose.metrics.util;

import org.junit.Assert;
import org.junit.Test;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

/**
 @author veronika K. on 16.10.18 */
public class RateMeterTest {

	final private static int PERIOD_SEC = 1;
	final private static int INTERVALS = 100;
	final private static int SLEEP_MILLISEC = 100;
	final private static int ELAPSED_TIME = SLEEP_MILLISEC * INTERVALS;

	@Test
	public void test()
	throws InterruptedException {
		final RateMeterImpl meter = new RateMeterImpl(Clock.systemDefaultZone(), PERIOD_SEC, "SOME_RATE");
		for(int i = 0; i < INTERVALS; ++ i) {
			meter.mark(); //mark(n), n = 1;
			TimeUnit.MILLISECONDS.sleep(SLEEP_MILLISEC);
		}
		//
		Assert.assertEquals(meter.count(), INTERVALS);
		//
		//meter return rates per micros
		final double expectedRateMicros = ((double) INTERVALS / TimeUnit.MILLISECONDS.toSeconds(ELAPSED_TIME)) *
			TimeUnit.SECONDS.toMicros(1);
		Assert.assertEquals(meter.meanRate(), expectedRateMicros, expectedRateMicros * 0.1);
		Assert.assertEquals(meter.lastRate(), expectedRateMicros, expectedRateMicros * 0.1);
		//
		Assert.assertEquals(meter.elapsedTimeMillis() > ELAPSED_TIME, true);
		Assert.assertEquals(meter.elapsedTimeMillis(), ELAPSED_TIME, ELAPSED_TIME * 0.2);
		//
		meter.resetStartTime();
		Assert.assertEquals(meter.elapsedTimeMillis(), 0);
	}
}
