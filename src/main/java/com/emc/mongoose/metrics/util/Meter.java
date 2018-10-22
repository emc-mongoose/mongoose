package com.emc.mongoose.metrics.util;

import com.emc.mongoose.metrics.snapshot.SingleMetricSnapshot;

/**
 @author veronika K. on 11.10.18 */
public interface Meter<S extends SingleMetricSnapshot> {

	String name();

	long count();

	S snapshot();
}
