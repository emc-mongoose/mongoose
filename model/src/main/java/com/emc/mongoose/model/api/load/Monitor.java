package com.emc.mongoose.model.api.load;

import com.emc.mongoose.common.concurrent.InterruptibleDaemon;
import com.emc.mongoose.model.api.io.Output;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.metrics.IoStats;

/**
 Created on 11.07.16.
 */
public interface Monitor<I extends Item, O extends IoTask<I>>
extends InterruptibleDaemon, Output<O>, Registry<Driver<I, O>> {
	
	IoStats.Snapshot getIoStatsSnapshot();

	String getName();
	
	void setItemOutput(final Output<I> itemOutput);
}
