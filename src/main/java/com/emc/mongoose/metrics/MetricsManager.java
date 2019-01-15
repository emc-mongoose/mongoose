package com.emc.mongoose.metrics;

import com.emc.mongoose.metrics.context.MetricsContext;

import com.github.akurilov.fiber4j.Fiber;

public interface MetricsManager
extends Fiber {

	void register(final MetricsContext metricsCtx);

	void unregister(final MetricsContext metricsCtx);
}
