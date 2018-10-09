package com.emc.mongoose.metrics.util;

import io.prometheus.client.Collector;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 @author veronika K. on 09.10.18 */
public class MetricsCollectorTest {

	private final MetricCollector metricsCollector = new MetricCollector();
	private final int METRICS_COUNT = 7;    //sum, min, max, count, mean, loQ, hiQ
	private final long ITERATION_COUNT = 10_000_000;

	@Test
	public void test() {
		long sum = 0;
		for(long i = 0; i < ITERATION_COUNT; ++ i) {
			metricsCollector.update(i);
			sum += i;
		}
		final List<Collector.MetricFamilySamples.Sample> samples = metricsCollector.collect().get(0).samples;
		//
		Assert.assertEquals(metricsCollector.collect().size(), 1);
		Assert.assertEquals(samples.size(), METRICS_COUNT);
		//count
		Assert.assertEquals(new Double(samples.get(0).value).longValue(), ITERATION_COUNT);
		//sum
		Assert.assertEquals(new Double(samples.get(1).value).longValue(), sum);
		//max
		Assert.assertEquals(new Double(samples.get(2).value).longValue(), ITERATION_COUNT - 1);
		//min
		Assert.assertEquals(new Double(samples.get(3).value).longValue(), 0);
		//mean
		final double mean = ((double) sum) / ITERATION_COUNT;
		Assert.assertEquals(new Double(samples.get(4).value).longValue(), mean, mean * 0.05);
		//TODO: quantieles
	}
}
