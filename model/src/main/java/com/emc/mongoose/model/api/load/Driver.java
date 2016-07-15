package com.emc.mongoose.model.api.load;

import com.emc.mongoose.common.concurrent.LifeCycle;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;

import java.io.Closeable;
import java.util.List;

/**
 Created on 11.07.16.
 */
public interface Driver<I extends Item, O extends IoTask<I>>
extends Closeable, LifeCycle {

	boolean isFullThrottleEntered();

	boolean isFullThrottleExited();

	boolean submit(final O task);

	int submit(final List<O> tasks, final int from, final int to);

	void registerMonitor(final Monitor<I, O> monitor)
	throws IllegalStateException;
}
