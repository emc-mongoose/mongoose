package com.emc.mongoose.base.load.step.client.metrics;

import com.emc.mongoose.base.metrics.snapshot.AllMetricsSnapshot;
import com.github.akurilov.commons.concurrent.AsyncRunnable;
import java.util.List;

public interface MetricsAggregator extends AsyncRunnable {

	List<AllMetricsSnapshot> metricsSnapshotsByIndex(final int originIndex);
}
