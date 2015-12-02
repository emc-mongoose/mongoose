package com.emc.mongoose.server.api.load.executor;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.executor.ContainerLoadExecutor;
/**
 Created by kurila on 21.10.15.
 */
public interface ContainerLoadSvc<T extends DataItem, C extends Container<T>>
extends LoadSvc<C>, ContainerLoadExecutor<T, C> {
}
