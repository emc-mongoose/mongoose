package com.emc.mongoose.scenario.step.master;

import com.emc.mongoose.api.metrics.MetricsSnapshot;
import com.emc.mongoose.scenario.step.LoadStep;

import java.util.List;

public interface LoadStepClient
extends LoadStep {

	List<MetricsSnapshot> remoteMetricsSnapshots(final int originIndex);
}
