package com.emc.mongoose.metrics.util;

/**
 @author veronika K. on 03.10.18 */
interface LongReservoir {

	int size();

	void update(final long value);

	long[] snapshot();
}
