package com.emc.mongoose.model.api.load;

import com.emc.mongoose.common.concurrent.InterruptableDaemon;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;

import java.io.Closeable;

/**
 Created on 11.07.16.
 */
public interface Generator<I extends Item, O extends IoTask<I>>
extends Closeable, InterruptableDaemon {

	void registerMonitor(final Monitor<I, O> monitor)
	throws IllegalStateException;
}
