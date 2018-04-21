package com.emc.mongoose.scenario.step;

import com.emc.mongoose.api.metrics.MetricsSnapshot;

import com.github.akurilov.concurrent.coroutines.Coroutine;

import java.util.List;
import java.util.function.Supplier;

public interface MetricsSnapshotsSupplierCoroutine
extends Supplier<List<MetricsSnapshot>>, Coroutine {

}
