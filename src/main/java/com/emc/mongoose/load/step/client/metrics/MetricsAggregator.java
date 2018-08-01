package com.emc.mongoose.load.step.client.metrics;

import com.emc.mongoose.metrics.MetricsSnapshot;

import com.github.akurilov.commons.concurrent.AsyncRunnable;

import java.util.List;

public interface MetricsAggregator
extends AsyncRunnable {

	List<MetricsSnapshot> metricsSnapshotsByIndex(final int originIndex);
}
