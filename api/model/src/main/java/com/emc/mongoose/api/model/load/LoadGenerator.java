package com.emc.mongoose.api.model.load;

import com.emc.mongoose.api.common.concurrent.Daemon;
import com.emc.mongoose.api.common.concurrent.Throttle;
import com.emc.mongoose.api.common.concurrent.WeightThrottle;
import com.emc.mongoose.api.common.io.Output;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.io.IoType;

/**
 Created on 11.07.16.
 */
public interface LoadGenerator<I extends Item, O extends IoTask<I>>
extends Daemon {
	
	void setWeightThrottle(final WeightThrottle weightThrottle);

	void setRateThrottle(final Throttle<Object> rateThrottle);

	void setOutput(final Output<O> ioTaskOutput);

	long getGeneratedIoTasksCount();

	long getTransferSizeEstimate();

	IoType getIoType();
	
	int getBatchSize();
}
