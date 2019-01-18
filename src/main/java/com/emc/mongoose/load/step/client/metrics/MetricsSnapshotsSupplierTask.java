package com.emc.mongoose.load.step.client.metrics;

import com.emc.mongoose.metrics.snapshot.AllMetricsSnapshot;
import com.github.akurilov.fiber4j.Fiber;
import java.util.List;
import java.util.function.Supplier;

public interface MetricsSnapshotsSupplierTask
    extends Supplier<List<? extends AllMetricsSnapshot>>, Fiber {}
