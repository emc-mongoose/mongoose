package com.emc.mongoose.client.api.load.builder;
//
import com.emc.mongoose.client.api.load.executor.ContainerLoadClient;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.DataItem;
/**
 Created by kurila on 21.10.15.
 */
public interface ContainerLoadBuilderClient<
	T extends DataItem, C extends Container<T>, U extends ContainerLoadClient<T, C>
> extends LoadBuilderClient<C, U> {
}
