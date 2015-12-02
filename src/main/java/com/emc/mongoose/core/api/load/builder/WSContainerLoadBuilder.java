package com.emc.mongoose.core.api.load.builder;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.load.executor.WSContainerLoadExecutor;
/**
 Created by kurila on 21.10.15.
 */
public interface WSContainerLoadBuilder<
	T extends WSObject, C extends Container<T>, U extends WSContainerLoadExecutor<T, C>
> extends ContainerLoadBuilder<T, C, U> {
}
