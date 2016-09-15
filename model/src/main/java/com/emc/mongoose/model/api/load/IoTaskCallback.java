package com.emc.mongoose.model.api.load;

import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;

import java.util.List;

/**
 Created by kurila on 14.09.16.
 */
public interface IoTaskCallback<I extends Item, O extends IoTask<I>> {
	
	void ioTaskCompleted(final O ioTask);
	
	int ioTaskCompletedBatch(final List<O> ioTasks, final int from, final int to);
}
