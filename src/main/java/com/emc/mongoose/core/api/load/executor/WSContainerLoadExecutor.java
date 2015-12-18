package com.emc.mongoose.core.api.load.executor;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.WSObject;
/**
 Created by kurila on 21.10.15.
 */
public interface WSContainerLoadExecutor<T extends WSObject, C extends Container<T>>
extends ContainerLoadExecutor<T, C> {
}
