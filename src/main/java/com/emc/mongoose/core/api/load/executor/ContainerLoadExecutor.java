package com.emc.mongoose.core.api.load.executor;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.container.Container;
/**
 Created by kurila on 21.10.15.
 */
public interface ContainerLoadExecutor<T extends Item, C extends Container<T>>
extends LoadExecutor<C> {
}
