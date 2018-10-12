package com.emc.mongoose.metrics.util;

/**
 @author veronika K. on 03.10.18 */
public interface RateMeter
	extends Meter {

	void resetStartTime();
	//

	/**
	 Mark the occurrence of an event.
	 */
	void mark();

	/**
	 Mark the occurrence of a given number of events.

	 @param n the number of events
	 */
	void mark(final long n);

	double meanRate();

	double lastRate();
}
