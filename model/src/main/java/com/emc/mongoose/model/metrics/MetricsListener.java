package com.emc.mongoose.model.metrics;

/**
 Created by andrey on 06.07.17.
 */
public interface MetricsListener {

	void notify(final MetricsContext.Snapshot snapshot);
}
