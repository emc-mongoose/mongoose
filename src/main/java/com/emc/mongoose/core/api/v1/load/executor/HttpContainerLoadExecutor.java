package com.emc.mongoose.core.api.v1.load.executor;
//
import com.emc.mongoose.core.api.v1.item.container.Container;
import com.emc.mongoose.core.api.v1.item.data.HttpDataItem;
/**
 Created by kurila on 21.10.15.
 */
public interface HttpContainerLoadExecutor<T extends HttpDataItem, C extends Container<T>>
extends ContainerLoadExecutor<T, C> {
}
