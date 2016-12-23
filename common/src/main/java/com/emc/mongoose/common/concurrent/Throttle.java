package com.emc.mongoose.common.concurrent;

/**
 Created by kurila on 29.03.16.
 Throttle can make a decision about the specified thing to pass or to wait.
 The throttle calls are not blocking so the caller should block if the throttle tells so.
 */
public interface Throttle<X> {

	/**
	 Request a decision about a thing
	 @param thing the subject of the decision
	 @return true if the thing should be passed, false otherwise
	 */
	boolean getPassFor(final X thing);

	/**
	 Request a decision about a set of things
	 @param thing the subject of the decision
	 @param times how many acquires is requested
	 @return how many acquires is got
	 */
	int getPassFor(final X thing, int times);
}
