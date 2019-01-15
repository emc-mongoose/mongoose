package com.emc.mongoose.metrics.snapshot;

import java.io.Serializable;

public interface DoubleLastMetricSnapshot extends Serializable {

  double last();
}
