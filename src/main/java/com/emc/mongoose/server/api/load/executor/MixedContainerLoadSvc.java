package com.emc.mongoose.server.api.load.executor;
//
import com.emc.mongoose.core.api.v1.item.base.Item;
import com.emc.mongoose.core.api.v1.item.container.Container;
/**
 Created by kurila on 01.04.16.
 */
public interface MixedContainerLoadSvc<T extends Item, C extends Container<T>>
extends ContainerLoadSvc<T, C>, MixedLoadSvc<C> {
}
