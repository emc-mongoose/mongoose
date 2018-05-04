package com.emc.mongoose.scenario.step.master.metrics;

import com.emc.mongoose.model.metrics.MetricsSnapshot;
import com.github.akurilov.concurrent.coroutine.Coroutine;

import java.util.List;
import java.util.function.Supplier;

public interface MetricsSnapshotsSupplierCoroutine
extends Supplier<List<MetricsSnapshot>>, Coroutine {

}
