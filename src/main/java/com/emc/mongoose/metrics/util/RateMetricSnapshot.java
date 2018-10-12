package com.emc.mongoose.metrics.util;

/**
 @author veronika K. on 12.10.18 */
public interface RateMetricSnapshot
	extends SingleMetricSnapshot {

	double last();

}
