package com.emc.mongoose.core.api.load.model;
//
import java.util.List;
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
	boolean requestApprovalFor(final X thing)
	throws InterruptedException;

	/**
	 Request a decision about a set of things
	 @param things the subject of the decision
	 @param from starting index
	 @param to ending index
	 @return true if the set of things should be passed, false otherwise
	 @throws InterruptedException
	 */
	boolean requestBatchApprovalFor(final List<X> things, int from, int to)
	throws InterruptedException;
}
