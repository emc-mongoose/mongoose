package com.emc.mongoose.api.model.load;

import com.emc.mongoose.api.common.concurrent.Daemon;
import com.emc.mongoose.api.common.io.Input;
import com.emc.mongoose.api.common.io.Output;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;

/**
 Created on 11.07.16.
 */
public interface LoadController<I extends Item, O extends IoTask<I>>
extends Daemon, Output<O> {
	
	String getName();
	
	void setIoResultsOutput(final Output<O> ioTaskResultsOutput);

	int getActiveTaskCount();
	
	default Input<O> getInput() {
		throw new AssertionError("Shouldn't be invoked");
	}
}
