package com.emc.mongoose.scenario.step.type;

import com.github.akurilov.concurrent.AsyncRunnable;

import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.io.IoType;

/**
 Created on 11.07.16.
 */
public interface LoadGenerator<I extends Item, O extends IoTask<I>>
extends AsyncRunnable {
	
	/**
	 @return sum of the new tasks and recycled ones
	 */
	long getGeneratedTasksCount();

	IoType getIoType();
	
	/**
	 @return true if the load generator is configured to recycle the tasks, false otherwise
	 */
	boolean isRecycling();

	/**
	 Enqueues the task for further recycling
	 @param ioTask the task to recycle
	 */
	void recycle(final O ioTask);
}
