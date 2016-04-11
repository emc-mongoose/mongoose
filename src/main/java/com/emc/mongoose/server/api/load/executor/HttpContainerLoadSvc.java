package com.emc.mongoose.server.api.load.executor;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.executor.HttpContainerLoadExecutor;
/**
 Created by kurila on 21.10.15.
 */
public interface HttpContainerLoadSvc<T extends HttpDataItem, C extends Container<T>>
extends ContainerLoadSvc<T, C>, HttpContainerLoadExecutor<T, C> {
}
