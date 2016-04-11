package com.emc.mongoose.core.api.load.builder;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.load.executor.ContainerLoadExecutor;
/**
 Created by kurila on 21.10.15.
 */
public interface ContainerLoadBuilder<
	T extends DataItem, C extends Container<T>, U extends ContainerLoadExecutor<T, C>
> extends LoadBuilder<C, U> {
}
