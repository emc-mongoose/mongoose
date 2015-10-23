package com.emc.mongoose.server.api.load.builder;
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.server.api.load.executor.WSContainerLoadSvc;
/**
 Created by kurila on 21.10.15.
 */
public interface WSContainerLoadBuilderSvc<
	T extends WSObject, C extends Container<T>, U extends WSContainerLoadSvc<T, C>
> extends ContainerLoadBuilderSvc<T, C, U> {
}
