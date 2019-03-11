package com.emc.mongoose.base.metrics.snapshot;

import java.io.Serializable;

public interface MeanMetricSnapshot extends Serializable {

	double mean();
}
