package com.emc.mongoose.metrics.context;

import com.emc.mongoose.metrics.snapshot.DistributedAllMetricsSnapshot;

public interface DistributedMetricsContext<S extends DistributedAllMetricsSnapshot>
extends MetricsContext<S> {

	int nodeCount();

	S lastSnapshot();
}
