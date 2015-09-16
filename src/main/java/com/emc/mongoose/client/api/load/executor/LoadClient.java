package com.emc.mongoose.client.api.load.executor;
// mongoose-common.jar
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.executor.LoadSvc;
//
import java.util.Map;
/**
 Created by andrey on 30.09.14.
 A client-side handler for controlling remote (server-size) load execution.
 */
public interface LoadClient<T extends DataItem>
extends LoadExecutor<T> {
	Map<String, LoadSvc<T>> getRemoteLoadMap();
}
