package com.emc.mongoose.model.api.load;

import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;

/**
 Created on 28.09.16.
 */
public interface LoadMonitorSvc<I extends Item, O extends IoTask<I>>
extends LoadMonitor<I, O>, Service {
	
}
