package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.metrics.MetricsSnapshot;

import java.util.List;

public interface LoadStepClient
extends LoadStep {

	List<MetricsSnapshot> remoteMetricsSnapshots(final int ioTypeCode);
}
