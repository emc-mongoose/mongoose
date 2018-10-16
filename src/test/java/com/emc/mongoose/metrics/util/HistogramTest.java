package com.emc.mongoose.metrics.util;

import org.junit.Assert;
import org.junit.Test;

/**
 @author veronika K. on 16.10.18 */
public class HistogramTest {

	private static final int RESERVOIR_SIZE = 1000;
	private static final long COUNT = RESERVOIR_SIZE;
	private static final double ACCURACY = 0.001;
	private static long sum = 0;

	@Test
	public void test() {
		final HistogramImpl histogram = new HistogramImpl(RESERVOIR_SIZE);
		for(int i = 0; i < COUNT; ++ i) {
			histogram.update(i * i);
			sum += i * i;
		}
		final HistogramSnapshot snapshot = histogram.snapshot();
		Assert.assertEquals(snapshot.count(), COUNT);
		Assert.assertEquals(snapshot.sum(), sum);
		Assert.assertEquals(snapshot.last(), (COUNT - 1) * (COUNT - 1));
		Assert.assertEquals(snapshot.max(), (COUNT - 1) * (COUNT - 1));
		Assert.assertEquals(snapshot.min(), 0);
		final long mean = sum / COUNT;
		Assert.assertEquals(snapshot.mean(), mean, mean * ACCURACY);
		final long med = ((COUNT / 2 - 1) * (COUNT / 2 - 1));
		Assert.assertEquals(((HistogramSnapshotImpl) snapshot).median(), med, med * ACCURACY);
		double qValue = 0.005;
		final long loQ = new Double(((COUNT * qValue - 1) * (COUNT * qValue - 1))).longValue();
		Assert.assertEquals(((HistogramSnapshotImpl) snapshot).quantile(qValue), loQ, loQ * ACCURACY);
		qValue = 0.995;
		final long hiQ = new Double(((COUNT * qValue - 1) * (COUNT * qValue - 1))).longValue();
		Assert.assertEquals(((HistogramSnapshotImpl) snapshot).quantile(qValue), hiQ, hiQ * ACCURACY);
	}
}
