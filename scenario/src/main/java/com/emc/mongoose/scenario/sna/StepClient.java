package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.metrics.MetricsSnapshot;

import java.util.List;

public interface StepClient
extends Step {

	List<MetricsSnapshot> remoteMetricsSnapshots(final int ioTypeCode);
}
