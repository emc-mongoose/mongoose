package com.emc.mongoose.server.api.load.executor;
//
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.executor.WSDataLoadExecutor;
/**
 Created by kurila on 01.10.14.
 */
public interface WSDataLoadSvc<T extends HttpDataItem>
extends WSDataLoadExecutor<T>, DataLoadSvc<T> {
}
