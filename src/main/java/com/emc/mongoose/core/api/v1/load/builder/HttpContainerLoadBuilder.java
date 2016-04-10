package com.emc.mongoose.core.api.v1.load.builder;
//
import com.emc.mongoose.core.api.v1.item.container.Container;
import com.emc.mongoose.core.api.v1.item.data.HttpDataItem;
import com.emc.mongoose.core.api.v1.load.executor.HttpContainerLoadExecutor;
/**
 Created by kurila on 21.10.15.
 */
public interface HttpContainerLoadBuilder<
	T extends HttpDataItem, C extends Container<T>, U extends HttpContainerLoadExecutor<T, C>
> extends ContainerLoadBuilder<T, C, U> {
}
