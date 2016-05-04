package com.emc.mongoose.client.api.load.executor;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.executor.HttpDataLoadExecutor;
import com.emc.mongoose.server.api.load.executor.HttpDataLoadSvc;
/**
 Created by kurila on 01.10.14.
 */
public interface HttpDataLoadClient<T extends HttpDataItem, W extends HttpDataLoadSvc<T>>
extends DataLoadClient<T, W>, HttpDataLoadExecutor<T> {
}
