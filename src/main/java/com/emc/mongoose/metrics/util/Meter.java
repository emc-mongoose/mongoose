package com.emc.mongoose.metrics.util;

/**
 @author veronika K. on 11.10.18 */
public interface Meter {

	String name();

	long count();

	SingleMetricSnapshot snapshot();
}
