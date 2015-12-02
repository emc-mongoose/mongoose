package com.emc.mongoose.client.api.load.executor;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.core.api.load.executor.WSContainerLoadExecutor;
import com.emc.mongoose.server.api.load.executor.WSContainerLoadSvc;
/**
 Created by kurila on 21.10.15.
 */
public interface WSContainerLoadClient<
	T extends WSObject, C extends Container<T>, W extends WSContainerLoadSvc<T, C>
> extends ContainerLoadClient<T, C, W>, WSContainerLoadExecutor<T, C> {
}
