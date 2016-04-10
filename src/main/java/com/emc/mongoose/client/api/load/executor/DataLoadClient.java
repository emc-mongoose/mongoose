package com.emc.mongoose.client.api.load.executor;
//
import com.emc.mongoose.core.api.v1.item.data.DataItem;
import com.emc.mongoose.server.api.load.executor.DataLoadSvc;
/**
 Created by kurila on 21.10.15.
 */
public interface DataLoadClient<T extends DataItem, W extends DataLoadSvc<T>>
extends LoadClient<T, W> {
}
