package com.emc.mongoose.model.load;

import com.emc.mongoose.common.concurrent.Daemon;
import com.emc.mongoose.common.io.Output;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;

import java.util.List;

/**
 Created on 11.07.16.
 */
public interface LoadMonitor<R extends IoResult>
extends Daemon {
	
	String getName();
	
	void setItemInfoOutput(final Output<String> itemInfoOutput);

	void processIoResults(final List<R> ioTaskResults, final int n, final boolean isCircular);
}
