package com.emc.mongoose.base.metrics.snapshot;

import java.io.Serializable;

public interface ElapsedTimeMetricSnapshot extends Serializable {

	long elapsedTimeMillis();
}
