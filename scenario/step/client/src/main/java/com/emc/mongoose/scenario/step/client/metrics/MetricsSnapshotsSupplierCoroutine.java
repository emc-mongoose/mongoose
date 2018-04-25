package com.emc.mongoose.scenario.step.client.metrics;

import com.emc.mongoose.api.metrics.MetricsSnapshot;
import com.github.akurilov.concurrent.coroutine.Coroutine;

import java.util.List;
import java.util.function.Supplier;

public interface MetricsSnapshotsSupplierCoroutine
extends Supplier<List<MetricsSnapshot>>, Coroutine {

}
