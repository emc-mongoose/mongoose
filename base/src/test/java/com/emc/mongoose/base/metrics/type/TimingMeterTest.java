package com.emc.mongoose.base.metrics.type;

import com.emc.mongoose.base.metrics.snapshot.TimingMetricSnapshot;
import com.emc.mongoose.base.metrics.util.ConcurrentSlidingWindowLongReservoir;
import org.junit.Assert;
import org.junit.Test;

/** @author veronika K. on 17.10.18 */
public class TimingMeterTest {

	private static final int INTERVALS = 100;

	@Test
	public void test() {
		final LongMeter<TimingMetricSnapshot> meter = new TimingMeterImpl(
						new HistogramImpl(new ConcurrentSlidingWindowLongReservoir()), "SOME_METRIC");
		int sum = 0;
		for (int i = 0; i < INTERVALS; ++i) {
			meter.update(i);
			sum += i;
		}
		//
		final TimingMetricSnapshot snapshot = meter.snapshot();
		Assert.assertEquals(snapshot.count(), INTERVALS);
		Assert.assertEquals(snapshot.sum(), sum);
		Assert.assertEquals(snapshot.min(), 0);
		final double mean = ((double) sum) / INTERVALS;
		Assert.assertEquals(snapshot.mean(), mean, mean * 0.001);
		Assert.assertEquals(snapshot.max(), INTERVALS - 1);
	}
}
