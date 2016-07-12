package com.emc.mongoose.monitor;

import com.emc.mongoose.common.concurrent.LifeCycleBase;
import com.emc.mongoose.common.io.IoTask;
import com.emc.mongoose.common.item.Item;
import com.emc.mongoose.common.load.Monitor;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 12.07.16.
 */
public class MonitorMock<I extends Item, O extends IoTask<I>>
extends LifeCycleBase
implements Monitor<I, O> {
	
	@Override
	public void ioTaskCompleted(final O ioTask) {
	}

	@Override
	public int ioTaskCompletedBatch(final List<O> ioTasks, final int from, final int to) {
		return 0;
	}

	@Override
	public boolean await()
	throws InterruptedException {
		return false;
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		return false;
	}
}
