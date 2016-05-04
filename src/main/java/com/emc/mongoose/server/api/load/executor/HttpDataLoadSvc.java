package com.emc.mongoose.server.api.load.executor;
//
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.executor.HttpDataLoadExecutor;
/**
 Created by kurila on 01.10.14.
 */
public interface HttpDataLoadSvc<T extends HttpDataItem>
extends HttpDataLoadExecutor<T>, DataLoadSvc<T> {
}
