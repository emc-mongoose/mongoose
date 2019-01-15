package com.emc.mongoose.metrics.context;

import com.emc.mongoose.metrics.snapshot.DistributedAllMetricsSnapshot;

import java.util.List;

public interface DistributedMetricsContext<S extends DistributedAllMetricsSnapshot>
extends MetricsContext<S> {

	int nodeCount();

	List<String> nodeAddrs();

	List<Double> quantileValues();

	S lastSnapshot();
}
