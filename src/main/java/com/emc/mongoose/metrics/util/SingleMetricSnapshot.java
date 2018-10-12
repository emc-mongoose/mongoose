package com.emc.mongoose.metrics.util;

import java.io.Serializable;

/**
 @author veronika K. on 12.10.18 */
public interface SingleMetricSnapshot
	extends Serializable {

	String name();

	double mean();

	long count();
}
