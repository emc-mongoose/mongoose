package com.emc.mongoose.metrics;

import com.github.akurilov.fiber4j.Fiber;

public interface MetricsManager
extends Fiber {

	void register(final String id, final MetricsContext metricsCtx);

	void unregister(final String id, final MetricsContext metricsCtx);
}
