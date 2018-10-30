package com.emc.mongoose.perf.type;

import com.emc.mongoose.metrics.snapshot.RateMetricSnapshot;
import com.emc.mongoose.metrics.type.RateMeter;
import com.emc.mongoose.metrics.type.RateMeterImpl;
import org.junit.Assert;
import org.junit.Test;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

/**
 @author veronika K. on 16.10.18 */
public class RateMeterTest {

	final private static int INTERVALS = 100;
	final private static int SLEEP_MILLISEC = 100;
	final private static int ELAPSED_TIME = SLEEP_MILLISEC * INTERVALS;

	@Test
	public void test()
	throws InterruptedException {
		final RateMeter<RateMetricSnapshot> meter = new RateMeterImpl(Clock.systemUTC(), "SOME_RATE");
		for(int i = 0; i < INTERVALS; ++ i) {
			meter.update(1);
			TimeUnit.MILLISECONDS.sleep(SLEEP_MILLISEC);
		}
		//
		RateMetricSnapshot snapshot = meter.snapshot();
		Assert.assertEquals(snapshot.count(), INTERVALS);
		//meter return rates per sec
		final double expectedRate = ((double) INTERVALS / TimeUnit.MILLISECONDS.toSeconds(ELAPSED_TIME));
		Assert.assertEquals(snapshot.mean(), expectedRate, expectedRate * 0.1);
		Assert.assertEquals(snapshot.last(), expectedRate, expectedRate * 0.1);
		//
		Assert.assertEquals(snapshot.elapsedTimeMillis() > ELAPSED_TIME, true);
		Assert.assertEquals(snapshot.elapsedTimeMillis(), ELAPSED_TIME, ELAPSED_TIME * 0.2);
		//
		meter.resetStartTime();
		snapshot = meter.snapshot();
		Assert.assertEquals(snapshot.elapsedTimeMillis(), 0);
	}
}
