package com.emc.mongoose.common.load;

import com.emc.mongoose.common.concurrent.LifeCycle;
import com.emc.mongoose.common.io.IoTask;
import com.emc.mongoose.common.item.Item;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 Created on 11.07.16.
 */
public interface Driver<I extends Item, O extends IoTask<I>>
extends LifeCycle {

	boolean isFullThrottleEntered();

	boolean isFullThrottleExited();

	Future<O> submit(final O task)
	throws RejectedExecutionException;

	int submit(final List<O> tasks, final int from, final int to)
	throws RejectedExecutionException;
}
