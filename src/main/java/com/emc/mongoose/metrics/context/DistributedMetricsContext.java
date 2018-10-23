package com.emc.mongoose.metrics.context;

import com.emc.mongoose.metrics.DistributedMetricsListener;
import com.emc.mongoose.metrics.snapshot.DistributedMetricsSnapshot;

import java.util.List;

public interface DistributedMetricsContext<S extends DistributedMetricsSnapshot>
extends MetricsContext<S> {

	int nodeCount();

	List<Double> quantileValues();

	S lastSnapshot();

	void metricsListener(final DistributedMetricsListener metricsListener);
}
