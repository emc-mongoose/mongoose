package com.emc.mongoose.server.api.load.builder;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.builder.HttpContainerLoadBuilder;
import com.emc.mongoose.server.api.load.executor.HttpContainerLoadSvc;
/**
 Created by kurila on 21.10.15.
 */
public interface HttpContainerLoadBuilderSvc<
	T extends HttpDataItem, C extends Container<T>, U extends HttpContainerLoadSvc<T, C>
>
extends HttpContainerLoadBuilder<T, C, U>, ContainerLoadBuilderSvc<T, C, U> {
}
