package com.emc.mongoose.client.api.load.builder;
//
import com.emc.mongoose.client.api.load.executor.WSContainerLoadClient;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.WSObject;
/**
 Created by kurila on 21.10.15.
 */
public interface WSContainerLoadBuilderClient<
	T extends WSObject, C extends Container<T>, U extends WSContainerLoadClient<T, C>
>
extends ContainerLoadBuilderClient<T, C, U> {
}
