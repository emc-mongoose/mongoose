package com.emc.mongoose.base.metrics.snapshot;

import java.io.Serializable;

public interface LongLastMetricSnapshot extends Serializable {

	long last();
}
