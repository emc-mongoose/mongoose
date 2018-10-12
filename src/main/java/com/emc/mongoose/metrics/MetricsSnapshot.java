package com.emc.mongoose.metrics;

import com.emc.mongoose.metrics.util.RateMetricSnapshot;
import com.emc.mongoose.metrics.util.TimingMetricSnapshot;

import java.io.Serializable;

public interface MetricsSnapshot
	extends Serializable {

	TimingMetricSnapshot durationSnapshot();

	TimingMetricSnapshot latencySnapshot();

	TimingMetricSnapshot concurrencySnapshot();

	RateMetricSnapshot byteSnapshot();

	RateMetricSnapshot successSnapshot();

	RateMetricSnapshot failsSnapshot();

	/**
	 @return value in milliseconds
	 */
	long elapsedTimeMillis();
}
