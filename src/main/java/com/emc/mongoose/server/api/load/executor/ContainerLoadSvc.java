package com.emc.mongoose.server.api.load.executor;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.load.executor.ContainerLoadExecutor;
/**
 Created by kurila on 21.10.15.
 */
public interface ContainerLoadSvc<T extends Item, C extends Container<T>>
extends LoadSvc<C>, ContainerLoadExecutor<T, C> {
}
