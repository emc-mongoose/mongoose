package com.emc.mongoose.metrics.snapshot;

import java.io.Serializable;

public interface NamedMetricSnapshot
extends Serializable {

	String name();
}
