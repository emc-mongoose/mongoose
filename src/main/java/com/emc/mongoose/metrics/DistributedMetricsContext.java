package com.emc.mongoose.metrics;

public interface DistributedMetricsContext<S extends DistributedMetricsSnapshot>
extends MetricsContext<S> {

	int nodeCount();

	S lastSnapshot();

	void metricsListener(final DistributedMetricsListener metricsListener);
}
