package com.emc.mongoose.model.api.load;

import com.emc.mongoose.common.concurrent.InterruptibleDaemon;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.metrics.IoStats;

import java.util.List;

/**
 Created on 11.07.16.
 */
public interface Monitor<I extends Item, O extends IoTask<I>>
extends InterruptibleDaemon {

	void ioTaskCompleted(final O ioTask);

	int ioTaskCompletedBatch(final List<O> ioTasks, final int from, final int to);

	void registerDriver(final Driver<I, O> driver)
	throws IllegalStateException;

	IoStats.Snapshot getIoStatsSnapshot();

	String getName();
}
