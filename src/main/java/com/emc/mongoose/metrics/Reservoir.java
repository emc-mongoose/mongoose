package com.emc.mongoose.metrics;

/**
 @author veronika K. on 03.10.18 */
interface Reservoir {

	int size();

	void update(final long value);

	Snapshot snapshot();
}
