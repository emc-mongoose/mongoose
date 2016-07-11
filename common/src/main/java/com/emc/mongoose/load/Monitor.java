package com.emc.mongoose.load;

import com.emc.mongoose.concurrent.LifeCycle;
import com.emc.mongoose.io.IoTask;
import com.emc.mongoose.item.Item;

import java.util.List;

/**
 Created on 11.07.16.
 */
public interface Monitor<I extends Item, O extends IoTask<I>>
extends LifeCycle {

	void ioTaskCompleted(final O ioTask);

	int ioTaskCompletedBatch(final List<O> ioTasks, final int from, final int to);
}
