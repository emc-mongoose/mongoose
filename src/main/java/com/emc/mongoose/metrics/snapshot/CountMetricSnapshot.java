package com.emc.mongoose.metrics.snapshot;

import java.io.Serializable;

/**
 @author veronika K. on 12.10.18 */
public interface LongMetricSnapshot
extends Serializable {

	String name();

	long count();

	double mean();
}
