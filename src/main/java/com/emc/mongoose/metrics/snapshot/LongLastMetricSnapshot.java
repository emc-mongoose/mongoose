package com.emc.mongoose.metrics.snapshot;

import java.io.Serializable;

public interface LongLastMetricSnapshot
extends Serializable {

	long last();
}
