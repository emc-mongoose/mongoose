package com.emc.mongoose.core.api.v1.load.barrier;

/**
 Created by kurila on 29.03.16.
 Barrier can make a decision about the specified thing. The decision may be one of "pass", "deny".
 Also barrier may wait (block the caller) before making the decision.
 */
public interface Barrier<X> {

	/**
	 Request a decision about a thing
	 @param thing the subject of the decision
	 @return true if the thing should be passed, false otherwise
	 @throws InterruptedException
	 */
	boolean getApprovalFor(final X thing)
	throws InterruptedException;

	/**
	 Request a decision about a set of things
	 @param thing the subject of the decision
	 @param times how many times the decision is true
	 @return true if the set of things should be passed, false otherwise
	 @throws InterruptedException
	 */
	boolean getApprovalsFor(final X thing, int times)
	throws InterruptedException;
}
