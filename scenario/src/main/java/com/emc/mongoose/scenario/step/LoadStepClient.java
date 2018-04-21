package com.emc.mongoose.scenario.step;

import com.emc.mongoose.api.metrics.MetricsSnapshot;

import java.util.List;

public interface LoadStepClient
extends LoadStep {

	List<MetricsSnapshot> remoteMetricsSnapshots(final int originIndex);
}
