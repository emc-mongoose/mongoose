package com.emc.mongoose.core.api.load.builder;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.executor.HttpContainerLoadExecutor;
/**
 Created by kurila on 21.10.15.
 */
public interface HttpContainerLoadBuilder<
	T extends HttpDataItem, C extends Container<T>, U extends HttpContainerLoadExecutor<T, C>
> extends ContainerLoadBuilder<T, C, U> {
}
