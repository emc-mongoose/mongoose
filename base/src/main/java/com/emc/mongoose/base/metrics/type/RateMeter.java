package com.emc.mongoose.base.metrics.type;

import com.emc.mongoose.base.metrics.snapshot.RateMetricSnapshot;

/** @author veronika K. on 03.10.18 */
public interface RateMeter<S extends RateMetricSnapshot> extends LongMeter<S> {

	int DEFAULT_PERIOD_SECONDS = 1;

	void resetStartTime();
}
