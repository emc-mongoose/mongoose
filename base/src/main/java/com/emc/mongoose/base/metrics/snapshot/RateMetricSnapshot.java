package com.emc.mongoose.base.metrics.snapshot;

/** @author veronika K. on 12.10.18 */
public interface RateMetricSnapshot
				extends CountMetricSnapshot,
				DoubleLastMetricSnapshot,
				ElapsedTimeMetricSnapshot,
				MeanMetricSnapshot,
				NamedMetricSnapshot {}
