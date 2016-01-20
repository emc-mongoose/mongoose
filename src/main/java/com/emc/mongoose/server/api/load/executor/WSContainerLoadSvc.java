package com.emc.mongoose.server.api.load.executor;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.executor.WSContainerLoadExecutor;
/**
 Created by kurila on 21.10.15.
 */
public interface WSContainerLoadSvc<T extends HttpDataItem, C extends Container<T>>
extends ContainerLoadSvc<T, C>, WSContainerLoadExecutor<T, C> {
}
