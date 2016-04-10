package com.emc.mongoose.server.api.load.executor;
import com.emc.mongoose.core.api.v1.item.container.Container;
import com.emc.mongoose.core.api.v1.item.data.HttpDataItem;
/**
 Created by kurila on 01.04.16.
 */
public interface MixedHttpContainerLoadSvc<T extends HttpDataItem, C extends Container<T>>
extends HttpContainerLoadSvc<T, C>, MixedContainerLoadSvc<T, C> {
}
