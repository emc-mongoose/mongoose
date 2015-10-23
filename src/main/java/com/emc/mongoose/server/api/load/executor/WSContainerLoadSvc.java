package com.emc.mongoose.server.api.load.executor;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.load.executor.WSContainerLoadExecutor;
/**
 Created by kurila on 21.10.15.
 */
public interface WSContainerLoadSvc<T extends WSObject, C extends Container<T>>
extends ContainerLoadSvc<T, C>, WSContainerLoadExecutor<T, C> {
}
