package com.emc.mongoose.core.api.v1.load.builder;
//
import com.emc.mongoose.core.api.v1.item.container.Container;
import com.emc.mongoose.core.api.v1.item.data.DataItem;
import com.emc.mongoose.core.api.v1.load.executor.ContainerLoadExecutor;
/**
 Created by kurila on 21.10.15.
 */
public interface ContainerLoadBuilder<
	T extends DataItem, C extends Container<T>, U extends ContainerLoadExecutor<T, C>
> extends LoadBuilder<C, U> {
}
