package com.emc.mongoose.client.api.load.executor;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
//
import com.emc.mongoose.server.api.load.executor.ContainerLoadSvc;
/**
 Created by kurila on 21.10.15.
 */
public interface ContainerLoadClient<
	T extends DataItem, C extends Container<T>, W extends ContainerLoadSvc<T, C>
> extends LoadClient<C, W> {
}
