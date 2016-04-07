package com.emc.mongoose.core.api.io.task;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
/**
 Created by kurila on 21.10.15.
 */
public interface HttpContainerIoTask<
	T extends HttpDataItem, C extends Container<T>
> extends HttpIoTask<C, HttpContainerIoTask<T, C>>, ContainerIoTask<T, C> {
}
