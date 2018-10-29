package com.emc.mongoose.metrics.snapshot;

import java.io.Serializable;

public interface ElapsedTimeMetricSnapshot
extends Serializable {

	long elapsedTimeMillis();
}
