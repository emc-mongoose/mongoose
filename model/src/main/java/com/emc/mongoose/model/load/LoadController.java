package com.emc.mongoose.model.load;

import com.emc.mongoose.common.concurrent.Daemon;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;

/**
 Created on 11.07.16.
 */
public interface LoadController<I extends Item, O extends IoTask<I>>
extends Daemon, Output<O> {
	
	long STATS_REFRESH_PERIOD_NANOS = 100_000;
	
	String getName();
	
	void setIoResultsOutput(final Output<O> ioTaskResultsOutput);

	int getActiveTaskCount();
	
	default Input<O> getInput() {
		throw new AssertionError("Shouldn't be invoked");
	}
}
