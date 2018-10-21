package com.emc.mongoose.metrics.type;

import com.emc.mongoose.metrics.snapshot.HistogramSnapshot;

/**
 @author veronika K. on 10.10.18 */
public interface Histogram {

	void update(final int value);

	void update(final long value);

	long count();

	HistogramSnapshot snapshot();
}
