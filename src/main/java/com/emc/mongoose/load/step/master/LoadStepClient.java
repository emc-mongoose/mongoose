package com.emc.mongoose.load.step.master;

import com.emc.mongoose.metrics.MetricsSnapshot;
import com.emc.mongoose.load.step.LoadStep;

import java.util.List;

public interface LoadStepClient
extends LoadStep {

	List<MetricsSnapshot> remoteMetricsSnapshots(final int originIndex);
}
