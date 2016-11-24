package com.emc.mongoose.model.load;

import com.emc.mongoose.common.net.Service;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
/**
 Created by andrey on 05.10.16.
 */
public interface LoadMonitorSvc<R extends IoResult>
extends LoadMonitor<R>, Service {
}
