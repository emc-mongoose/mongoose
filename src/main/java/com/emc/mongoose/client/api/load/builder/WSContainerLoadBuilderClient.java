package com.emc.mongoose.client.api.load.builder;
//
import com.emc.mongoose.client.api.load.executor.WSContainerLoadClient;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.load.builder.WSContainerLoadBuilder;
import com.emc.mongoose.server.api.load.executor.WSContainerLoadSvc;
/**
 Created by kurila on 21.10.15.
 */
public interface WSContainerLoadBuilderClient<
	T extends WSObject,
	C extends Container<T>,
	W extends WSContainerLoadSvc<T, C>,
	U extends WSContainerLoadClient<T, C, W>
> extends WSContainerLoadBuilder<T, C, U>, ContainerLoadBuilderClient<T, C, W, U> {
}
