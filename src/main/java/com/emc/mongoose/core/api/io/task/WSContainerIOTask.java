package com.emc.mongoose.core.api.io.task;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.WSObject;
/**
 Created by kurila on 21.10.15.
 */
public interface WSContainerIOTask<T extends WSObject, C extends Container<T>>
extends WSIOTask<C, WSContainerIOTask<T, C>>, ContainerIOTask<T, C> {
}
