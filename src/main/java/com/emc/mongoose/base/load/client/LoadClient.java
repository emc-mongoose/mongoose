package com.emc.mongoose.base.load.client;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.base.load.server.LoadSvc;

import java.util.Map;
/**
 Created by andrey on 30.09.14.
 A client-side handler for controlling remote (server-size) load execution.
 */
public interface LoadClient<T extends DataItem>
extends LoadExecutor<T> {
	Map<String, LoadSvc<T>> getRemoteLoadMap();
}
