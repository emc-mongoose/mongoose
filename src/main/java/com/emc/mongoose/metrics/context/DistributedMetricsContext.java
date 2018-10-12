package com.emc.mongoose.metrics.context;

import com.emc.mongoose.metrics.DistributedMetricsListener;
import com.emc.mongoose.metrics.DistributedMetricsSnapshot;

public interface DistributedMetricsContext<S extends DistributedMetricsSnapshot>
	extends MetricsContext<S> {

	int nodeCount();

	S lastSnapshot();

	void metricsListener(final DistributedMetricsListener metricsListener);
}
