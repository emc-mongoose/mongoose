package com.emc.mongoose.metrics.util;

import com.emc.mongoose.metrics.snapshot.NamedMetricSnapshot;

/**
 @author veronika K. on 11.10.18 */
public interface LongMeter<S extends NamedMetricSnapshot> {

	void update(final long v);

	S snapshot();
}
