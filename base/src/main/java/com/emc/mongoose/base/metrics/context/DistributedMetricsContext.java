package com.emc.mongoose.base.metrics.context;

import com.emc.mongoose.base.metrics.snapshot.DistributedAllMetricsSnapshot;

import java.util.List;

public interface DistributedMetricsContext<S extends DistributedAllMetricsSnapshot>
				extends MetricsContext<S> {

	int nodeCount();

	List<String> nodeAddrs();

	List<Double> quantileValues();

	S lastSnapshot();
}
