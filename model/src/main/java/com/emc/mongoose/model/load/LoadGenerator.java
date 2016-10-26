package com.emc.mongoose.model.load;

import com.emc.mongoose.common.concurrent.Daemon;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;

/**
 Created on 11.07.16.
 */
public interface LoadGenerator<I extends Item, O extends IoTask<I>>
extends Daemon {
}
