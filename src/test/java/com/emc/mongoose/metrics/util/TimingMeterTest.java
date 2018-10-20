package com.emc.mongoose.metrics.util;

import org.junit.Assert;
import org.junit.Test;

/**
 @author veronika K. on 17.10.18 */
public class TimingMeterTest {

	final private static int INTERVALS = 100;

	@Test
	public void test() {
		final TimingMeter meter = new TimingMeterImpl<>(
			new HistogramImpl(new ConcurrentSlidingWindowLongReservoir()), "SOME_METRIC"
		);
		int sum = 0;
		for(int i = 0; i < INTERVALS; ++ i) {
			meter.update(i);
			sum += i;
		}
		//
		Assert.assertEquals(meter.count(), INTERVALS);
		Assert.assertEquals(meter.sum(), sum);
		Assert.assertEquals(meter.min(), 0);
		final double mean = ((double) sum) / INTERVALS;
		Assert.assertEquals(meter.mean(), mean, mean * 0.001);
		Assert.assertEquals(meter.max(), INTERVALS - 1);
	}
}