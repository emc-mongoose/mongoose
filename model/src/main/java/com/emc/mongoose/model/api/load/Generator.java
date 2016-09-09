package com.emc.mongoose.model.api.load;

import com.emc.mongoose.common.concurrent.InterruptibleDaemon;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;

/**
 Created on 11.07.16.
 */
public interface Generator<I extends Item, O extends IoTask<I>>
extends InterruptibleDaemon {

	void registerMonitor(final Monitor<I, O> monitor)
	throws IllegalStateException;
}
