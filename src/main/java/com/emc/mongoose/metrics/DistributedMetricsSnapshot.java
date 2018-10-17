package com.emc.mongoose.metrics;

/**
 @author veronika K. */
public interface DistributedMetricsSnapshot
extends MetricsSnapshot {

	/**
	 @return values in microseconds
	 */
	int nodeCount();
}
