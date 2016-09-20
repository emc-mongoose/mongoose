package com.emc.mongoose.model.api.load;

import com.emc.mongoose.common.concurrent.InterruptibleDaemon;
import com.emc.mongoose.model.api.io.Output;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;

/**
 Created on 11.07.16.
 */
public interface Driver<I extends Item, O extends IoTask<I>>
extends InterruptibleDaemon, Output<O>, Registry<Monitor<I, O>> {

	boolean isFullThrottleEntered();

	boolean isFullThrottleExited();
}
