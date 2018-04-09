package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.metrics.MetricsSnapshot;

import com.github.akurilov.concurrent.coroutines.Coroutine;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.util.function.Supplier;

public interface MetricsSnapshotsSupplierCoroutine
extends Supplier<Int2ObjectMap<MetricsSnapshot>>, Coroutine {

}
