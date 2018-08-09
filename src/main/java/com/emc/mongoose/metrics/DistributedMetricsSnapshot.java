package com.emc.mongoose.metrics;

public interface DistributedMetricsSnapshot
	extends MetricsSnapshot {

	/**
	 @return values in microseconds
	 */
	int nodeCount();
}
