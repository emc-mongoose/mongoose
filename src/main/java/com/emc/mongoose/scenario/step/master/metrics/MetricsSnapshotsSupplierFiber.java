package com.emc.mongoose.scenario.step.master.metrics;

import com.emc.mongoose.model.metrics.MetricsSnapshot;

import com.github.akurilov.fiber4j.Fiber;

import java.util.List;
import java.util.function.Supplier;

public interface MetricsSnapshotsSupplierFiber
extends Supplier<List<MetricsSnapshot>>, Fiber {

}
