package com.emc.mongoose.server.api.load.executor;
//
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.load.executor.DataLoadExecutor;
/**
 Created by kurila on 21.10.15.
 */
public interface DataLoadSvc<T extends DataItem>
extends LoadSvc<T>, DataLoadExecutor<T> {
}
