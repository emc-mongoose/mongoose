package com.emc.mongoose.client.api.load.builder;
//
import com.emc.mongoose.client.api.load.executor.HttpContainerLoadClient;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.builder.HttpContainerLoadBuilder;
import com.emc.mongoose.server.api.load.executor.HttpContainerLoadSvc;
/**
 Created by kurila on 21.10.15.
 */
public interface HttpContainerLoadBuilderClient<
	T extends HttpDataItem,
	C extends Container<T>,
	W extends HttpContainerLoadSvc<T, C>,
	U extends HttpContainerLoadClient<T, C, W>
> extends HttpContainerLoadBuilder<T, C, U>, ContainerLoadBuilderClient<T, C, W, U> {
}
