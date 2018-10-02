package com.emc.mongoose.metrics;

import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 @author veronika K. on 25.09.18 **/
public class PrometheusPrimitiveTest {

	@Test
	public final void testSummery() {
		final Summary someMetric = Summary
			.build()
			.quantile(0.5, 0.05)   // Add 50th percentile (= median) with 5% tolerated error
			.quantile(0.9, 0.01)   // Add 90th percentile with 1% tolerated error
			.quantile(0.999999, 0.001) // Add 99th percentile with 0.1% tolerated error
			.name("some_metric_s")
			.help("Measured metric.")
			.register();
		for(int i = 0; i < 10; ++ i) {
			someMetric.observe((double) i / 10);
		}
		assertEquals(someMetric.collect().size(),1);
		assertEquals(someMetric.collect().get(0).samples.size(),5);
		someMetric.collect().forEach(samples -> samples.samples.forEach(sample -> System.out.println(sample)));
		System.out.println("\n");

		final Summary otherMetric = Summary
			.build()
			.quantile(0.5, 0.05)   // Add 50th percentile (= median) with 5% tolerated error
			.quantile(0.95, 0.01)   // Add 90th percentile with 1% tolerated error
			.quantile(0.99, 0.001) // Add 99th percentile with 0.1% tolerated error
			.name("other_metric_s")
			.help("Other measured metric.")
			.register();
		for(int i = 0; i < 20; ++ i) {
			otherMetric.observe(i);
		}
		assertEquals(otherMetric.collect().size(),1);
		//assertEquals(otherMetric.collect().get(0).samples.size(),3);
		otherMetric.collect().forEach(samples -> samples.samples.forEach(sample -> System.out.println(sample)));
	}

	@Test
	public final void testHistogram() {
		final Histogram someMetric = Histogram
			.build()
			.linearBuckets(0.0,0.5,20)
			.name("some_metric_h")
			.help("Measured metric.")
			.register();
		for(int i = 0; i < 10; ++ i) {
			someMetric.observe(i);
		}
		assertEquals(someMetric.collect().size(),1);
		someMetric.collect().forEach(samples -> samples.samples.forEach(sample -> System.out.println(sample)));

		System.out.println("\n");

		final Histogram otherMetric = Histogram
			.build()
			.name("other_metric_h")
			.help("Other measured metric.")
			.register();
		for(int i = 0; i < 20; ++ i) {
			otherMetric.observe(i);
		}
		assertEquals(otherMetric.collect().size(),1);
		otherMetric.collect().forEach(samples -> samples.samples.forEach(sample -> System.out.println(sample)));
	}

	@Test
	public final void testMinMax() {
		final Summary metric1 = Summary
			.build()
			.quantile(Double.MIN_VALUE, 0.0)  // min
			.quantile(1.0, 0.0)  // max
			.name("metric1")
			.help("Measured metric.")
			.register();
		final Gauge min = Gauge.build().name("metric1_min").help("Measured metric.").register();
		final Gauge max = Gauge.build().name("metric1_max").help("Measured metric.").register();
		for(int i = 0; i < 10; ++ i) {
			final double value = (double) i / 10;
			if(min.get()>=value){
				min.set(value);
			}
			if(max.get()<=value){
				max.set(value);
			}
			metric1.observe(value);
		}
		min.collect().forEach(samples -> samples.samples.forEach(sample -> System.out.println(sample)));
		max.collect().forEach(samples -> samples.samples.forEach(sample -> System.out.println(sample)));
		metric1.collect().forEach(samples -> samples.samples.forEach(sample -> System.out.println(sample)));
		System.out.println("\n");
		for(int i = 2; i < 15; ++ i) {
			metric1.observe((double) i / 10);
		}
		metric1.collect().forEach(samples -> samples.samples.forEach(sample -> System.out.println(sample)));
		System.out.println("\n");

	}
}
