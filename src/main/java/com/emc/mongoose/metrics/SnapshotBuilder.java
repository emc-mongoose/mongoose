package com.emc.mongoose.metrics;

import com.codahale.metrics.Snapshot;
import com.codahale.metrics.UniformSnapshot;
import io.prometheus.client.Collector;
import io.prometheus.client.Histogram;

import java.util.ArrayList;
import java.util.List;

public class SnapshotBuilder {

	public static Snapshot build(final Histogram histogram) {
		final int s = histogram.collect().get(0).samples.size();
		final List<Collector.MetricFamilySamples.Sample> values = histogram.collect().get(0).samples;
		final List<Long> copy = new ArrayList<>(s);
		for(int i = 0; i < s; i++) {
			copy.add(new Double(values.get(i).value).longValue());
		}
		return new UniformSnapshot(copy);
	}
}
