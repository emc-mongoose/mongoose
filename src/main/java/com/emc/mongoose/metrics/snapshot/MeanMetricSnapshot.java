package com.emc.mongoose.metrics.snapshot;

import java.io.Serializable;

public interface MeanMetricSnapshot extends Serializable {

  double mean();
}
