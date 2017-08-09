package com.emc.mongoose.api.metrics;

/**
 Created by andrey on 06.07.17.
 The entity accepting the metrics updates
 */
public interface MetricsListener {

	/**
	 Update the state with the snapshot
	 @param snapshot the metrics snapshot to update the metrics listener state
	 */
	void notify(final MetricsContext.Snapshot snapshot);
}
