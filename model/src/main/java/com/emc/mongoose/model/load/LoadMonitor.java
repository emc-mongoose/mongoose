package com.emc.mongoose.model.load;

import com.emc.mongoose.common.concurrent.Daemon;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;

import java.io.IOException;
import java.util.List;

/**
 Created on 11.07.16.
 */
public interface LoadMonitor<I extends Item, O extends IoTask<I>>
extends Daemon {
	
	String getName();
	
	void setIoResultsOutput(final Output<O> ioTaskResultsOutput);

	void processIoResults(final List<O> ioTaskResults, final int n)
	throws IOException;
	
	int getActiveTaskCount();
}
