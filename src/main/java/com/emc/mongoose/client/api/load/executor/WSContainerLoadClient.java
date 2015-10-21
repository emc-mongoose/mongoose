package com.emc.mongoose.client.api.load.executor;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.WSObject;
/**
 Created by kurila on 21.10.15.
 */
public interface WSContainerLoadClient<T extends WSObject, C extends Container<T>>
extends ContainerLoadClient<T, C> {
}
