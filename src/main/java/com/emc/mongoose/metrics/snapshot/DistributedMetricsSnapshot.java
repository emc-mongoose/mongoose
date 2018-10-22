package com.emc.mongoose.metrics.snapshot;

/**
 @author veronika K. */
public interface DistributedMetricsSnapshot
extends MetricsSnapshot {

	/**
	 @return values in microseconds
	 */
	int nodeCount();
}
