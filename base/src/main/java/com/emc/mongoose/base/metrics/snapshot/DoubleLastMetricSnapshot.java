package com.emc.mongoose.base.metrics.snapshot;

import java.io.Serializable;

public interface DoubleLastMetricSnapshot extends Serializable {

	double last();
}
