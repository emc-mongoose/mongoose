package com.emc.mongoose.model.load;

import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.model.io.task.IoTask;

/**
 Created by andrey on 05.10.16.
 */
public interface LoadMonitorSvc<R extends IoTask.IoResult>
extends LoadMonitor<R>, Service {
}
