package com.emc.mongoose.load.step.master.metrics;

import com.emc.mongoose.metrics.MetricsSnapshot;

import com.github.akurilov.fiber4j.Fiber;

import java.util.List;
import java.util.function.Supplier;

public interface MetricsSnapshotsSupplierFiber
extends Supplier<List<MetricsSnapshot>>, Fiber {

}
