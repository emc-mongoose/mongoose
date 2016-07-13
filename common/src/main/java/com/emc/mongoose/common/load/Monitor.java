package com.emc.mongoose.common.load;

import com.emc.mongoose.common.concurrent.LifeCycle;
import com.emc.mongoose.common.io.IoTask;
import com.emc.mongoose.common.item.Item;

import java.io.Closeable;
import java.util.List;

/**
 Created on 11.07.16.
 */
public interface Monitor<I extends Item, O extends IoTask<I>>
extends Closeable, LifeCycle {

	void ioTaskCompleted(final O ioTask);

	int ioTaskCompletedBatch(final List<O> ioTasks, final int from, final int to);

	void registerDriver(final Driver<I, O> driver)
	throws IllegalStateException;

	void driverFinished(final Driver<I, O> driver);
}
