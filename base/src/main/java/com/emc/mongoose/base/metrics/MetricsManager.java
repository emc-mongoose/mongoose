package com.emc.mongoose.base.metrics;

import com.emc.mongoose.base.metrics.context.MetricsContext;
import com.github.akurilov.fiber4j.Fiber;

public interface MetricsManager extends Fiber {

	void register(final MetricsContext metricsCtx);

	void unregister(final MetricsContext metricsCtx);
}
