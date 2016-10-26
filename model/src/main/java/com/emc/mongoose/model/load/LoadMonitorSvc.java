package com.emc.mongoose.model.load;

import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;

/**
 Created by andrey on 05.10.16.
 */
public interface LoadMonitorSvc<I extends Item, O extends IoTask<I>>
extends LoadMonitor<I, O>, Service {
}
