package com.emc.mongoose.client.api.load.builder;
//
import com.emc.mongoose.client.api.load.executor.ContainerLoadClient;
//
import com.emc.mongoose.core.api.v1.item.container.Container;
import com.emc.mongoose.core.api.v1.item.data.DataItem;
import com.emc.mongoose.server.api.load.executor.ContainerLoadSvc;
/**
 Created by kurila on 21.10.15.
 */
public interface ContainerLoadBuilderClient<
	T extends DataItem,
	C extends Container<T>,
	W extends ContainerLoadSvc<T, C>,
	U extends ContainerLoadClient<T, C, W>
> extends LoadBuilderClient<C, W, U> {
}
