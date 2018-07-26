package com.emc.mongoose.load.generator;

import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.op.OpType;

import com.github.akurilov.fiber4j.Fiber;

/**
 Created on 11.07.16.
 */
public interface LoadGenerator<I extends Item, O extends Operation<I>>
extends Fiber {
	
	/**
	 @return sum of the new tasks and recycled ones
	 */
	long generatedOpCount();

	OpType opType();

	/**
	 * Returns true if the load generator is configured to recycle the load operations, false otherwise
	 */
	boolean isRecycling();
	
	/**
	 Enqueues the task for further recycling
	 @param op the task to recycle
	 */
	void recycle(final O op);
}
